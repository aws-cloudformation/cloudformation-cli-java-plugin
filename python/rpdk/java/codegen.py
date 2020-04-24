# pylint: disable=useless-super-delegation,too-many-locals
# pylint doesn't recognize abstract methods
import logging
import shutil
from collections import namedtuple

from rpdk.core.data_loaders import resource_stream
from rpdk.core.exceptions import InternalError, SysExitRecommendedError
from rpdk.core.init import input_with_validation
from rpdk.core.jsonutils.resolver import resolve_models
from rpdk.core.plugin_base import LanguagePlugin

from .resolver import translate_type
from .utils import safe_reserved, validate_codegen_model, validate_namespace

LOG = logging.getLogger(__name__)

OPERATIONS = ("Create", "Read", "Update", "Delete", "List")
EXECUTABLE = "cfn"
AWSCODEGEN = namedtuple("AWSCODEGEN", "default guided default_code guided_code")
CODEGEN = AWSCODEGEN("default", "guided_aws", "1", "2")


def logdebug(func: object):
    def wrapper(*args, **kwargs):
        log_msg = func.__name__ if not func.__doc__ else func.__doc__
        entry_message = "{} started".format(log_msg)
        LOG.debug(entry_message)
        if "entity" in kwargs:
            writing_message = "Writing {}".format(kwargs["entity"])
            LOG.debug(writing_message)
        result = func(*args, **kwargs)
        exit_message = "{} complete".format(log_msg)
        LOG.debug(exit_message)
        return result

    return wrapper


class JavaArchiveNotFoundError(SysExitRecommendedError):
    pass


class JavaLanguagePlugin(LanguagePlugin):
    MODULE_NAME = __name__
    RUNTIME = "java8"
    ENTRY_POINT = "{}.HandlerWrapper::handleRequest"
    TEST_ENTRY_POINT = "{}.HandlerWrapper::testEntrypoint"
    CODE_URI = "./target/{}-1.0-SNAPSHOT.jar"

    def __init__(self):
        self.env = self._setup_jinja_env(
            trim_blocks=True, lstrip_blocks=True, keep_trailing_newline=True
        )
        self.codegen_template_path = None
        self.env.filters["translate_type"] = translate_type
        self.env.filters["safe_reserved"] = safe_reserved
        self.namespace = None
        self.package_name = None

    def _namespace_from_project(self, project):
        try:
            self.namespace = project.settings["namespace"]
        except KeyError:
            # fallback provided to be backwards compatible
            fallback = ("com",) + project.type_info
            namespace = tuple(safe_reserved(s.lower()) for s in fallback)
            self.namespace = project.settings["namespace"] = namespace
            project._write_settings("java")  # pylint: disable=protected-access

        self.package_name = ".".join(self.namespace)

    def _prompt_for_namespace(self, project):
        if project.type_info[0] == "AWS":
            namespace = ("software", "amazon") + project.type_info[1:]
        else:
            namespace = ("com",) + project.type_info

        namespace = tuple(safe_reserved(s.lower()) for s in namespace)

        prompt = "Enter a package name (empty for default '{}'): ".format(
            ".".join(namespace)
        )

        self.namespace = input_with_validation(prompt, validate_namespace(namespace))
        project.settings["namespace"] = self.namespace
        self.package_name = ".".join(self.namespace)

    @staticmethod
    def _prompt_for_codegen_model(project):
        prompt = "Choose codegen model - 1 (default) or 2 (guided-aws): "

        codegen_model = input_with_validation(
            prompt, validate_codegen_model(CODEGEN.default_code)
        )

        project.settings["codegen_template_path"] = CODEGEN.default

        if codegen_model == CODEGEN.guided_code:
            project.settings["codegen_template_path"] = CODEGEN.guided

    def _get_template(self, project, stage, name):
        return self.env.get_template(
            stage + "/" + project.settings["codegen_template_path"] + "/" + name
        )

    @staticmethod
    def _is_aws_guided(project: object) -> bool:
        return project.settings["codegen_template_path"] == CODEGEN.guided

    @logdebug
    def _writing_component(
        self, project: object, src: str, entity: str, **kwargs  # noqa: C816
    ) -> None:
        """Writing module"""

        stub_entity: str = kwargs.get("stub_entity")
        operation: str = kwargs.get("operation")
        pojo_name: str = kwargs.get("pojo_name")
        call_graph: str = kwargs.get("call_graph")

        if not stub_entity:
            stub_entity = entity

        template = self._get_template(project, "init", stub_entity)
        path = src / entity
        contents = template.render(
            package_name=self.package_name,
            operation=operation,
            pojo_name=pojo_name,
            call_graph=call_graph,
        )
        project.safewrite(path, contents)

    @logdebug
    def init(self, project):
        """Init"""
        self._prompt_for_namespace(project)

        self._prompt_for_codegen_model(project)

        self._init_settings(project)

        # maven folder structure
        src = (project.root / "src" / "main" / "java").joinpath(*self.namespace)
        LOG.debug("Making source folder structure: %s", src)
        src.mkdir(parents=True, exist_ok=True)
        tst = (project.root / "src" / "test" / "java").joinpath(*self.namespace)
        LOG.debug("Making test folder structure: %s", tst)
        tst.mkdir(parents=True, exist_ok=True)

        # initialize shared files
        self.init_shared(project, src, tst)

        # write specialized generated files
        if self._is_aws_guided(project):
            self.init_guided_aws(project, src, tst)

    @logdebug
    def init_shared(self, project, src, tst):
        """Writing project configuration"""
        # .gitignore
        path = project.root / ".gitignore"
        LOG.debug("Writing .gitignore: %s", path)
        contents = resource_stream(__name__, "data/java.gitignore").read()
        project.safewrite(path, contents)

        # pom.xml
        path = project.root / "pom.xml"
        LOG.debug("Writing Maven POM: %s", path)
        template = self.env.get_template("init/shared/pom.xml")
        artifact_id = "{}-handler".format(project.hypenated_name)
        contents = template.render(
            group_id=self.package_name,
            artifact_id=artifact_id,
            executable=EXECUTABLE,
            schema_file_name=project.schema_filename,
            package_name=self.package_name,
        )
        project.safewrite(path, contents)

        # lombok.config
        path = project.root / "lombok.config"
        LOG.debug("Writing Lombok Config: %s", path)
        template = self.env.get_template("init/shared/lombok.config")
        contents = template.render()
        project.safewrite(path, contents)

        LOG.debug("Writing callback context")
        template = self.env.get_template("init/shared/CallbackContext.java")
        path = src / "CallbackContext.java"
        contents = template.render(package_name=self.package_name)
        project.safewrite(path, contents)

        path = src / "Configuration.java"
        LOG.debug("Writing configuration: %s", path)
        template = self.env.get_template("init/shared/StubConfiguration.java")
        contents = template.render(
            package_name=self.package_name,
            schema_file_name=project.schema_filename,
            pojo_name="ResourceModel",
        )
        project.safewrite(path, contents)

        # CloudFormation/SAM template for handler lambda
        path = project.root / "template.yml"
        LOG.debug("Writing SAM template: %s", path)
        template = self.env.get_template(
            "template.yml"
        )  # this template is in the CLI itself
        handler_params = {
            "Handler": project.entrypoint,
            "Runtime": project.runtime,
            "CodeUri": self.CODE_URI.format(artifact_id),
        }
        contents = template.render(
            resource_type=project.type_name,
            functions={
                "TypeFunction": handler_params,
                "TestEntrypoint": {
                    **handler_params,
                    "Handler": project.test_entrypoint,
                },
            },
        )
        project.safewrite(path, contents)

        # generate docs
        path = project.root / "README.md"
        LOG.debug("Writing README: %s", path)
        template = self.env.get_template("init/shared/README.md")
        contents = template.render(
            type_name=project.type_name,
            schema_path=project.schema_path,
            executable=EXECUTABLE,
        )
        project.safewrite(path, contents)

        self.init_handlers(project, src, tst)

    @logdebug
    def init_guided_aws(self, project, src, tst):
        """Writing supporting modules"""
        self._writing_component(project, src, entity="Translator.java")
        self._writing_component(project, src, entity="ClientBuilder.java")
        self._writing_component(project, src, entity="BaseHandlerStd.java")
        self._writing_component(project, tst, entity="AbstractTestBase.java")

    @logdebug
    def init_handlers(self, project, src, tst):
        """Writing stub handlers and tests"""
        pojo_name = "ResourceModel"
        for operation in OPERATIONS:
            entity = "{}Handler.java".format(operation)
            entity_test = "{}HandlerTest.java".format(operation)

            stub_entity = "Stub{}Handler.java".format(
                operation if operation == "List" or self._is_aws_guided(project) else ""
            )
            stub_entity_test = "Stub{}HandlerTest.java".format(
                operation if operation == "List" else ""
            )

            self._writing_component(
                project,
                src,
                entity=entity,
                stub_entity=stub_entity,
                operation=operation,
                pojo_name=pojo_name,
                call_graph=project.type_name.replace("::", "-"),
            )
            self._writing_component(
                project,
                tst,
                entity=entity_test,
                stub_entity=stub_entity_test,
                operation=operation,
                pojo_name=pojo_name,
            )

    def _init_settings(self, project):
        project.runtime = self.RUNTIME
        project.entrypoint = self.ENTRY_POINT.format(self.package_name)
        project.test_entrypoint = self.TEST_ENTRY_POINT.format(self.package_name)

    @staticmethod
    def _get_generated_root(project):
        return project.root / "target" / "generated-sources" / "rpdk"

    @staticmethod
    def _get_generated_tests_root(project):
        return project.root / "target" / "generated-test-sources" / "rpdk"

    @logdebug
    def generate(self, project):
        """Generate"""

        self._namespace_from_project(project)

        # clean generated files
        generated_root = self._get_generated_root(project)
        LOG.debug("Removing generated sources: %s", generated_root)
        shutil.rmtree(generated_root, ignore_errors=True)
        generated_tests_root = self._get_generated_tests_root(project)
        LOG.debug("Removing generated tests: %s", generated_tests_root)
        shutil.rmtree(generated_tests_root, ignore_errors=True)

        # create generated sources and tests directories
        src = generated_root.joinpath(*self.namespace)
        LOG.debug("Making generated folder structure: %s", src)
        src.mkdir(parents=True, exist_ok=True)

        test_src = generated_tests_root.joinpath(*self.namespace)
        LOG.debug("Making generated tests folder structure: %s", test_src)
        test_src.mkdir(parents=True, exist_ok=True)

        # write generated handler integration with LambdaWrapper
        path = src / "HandlerWrapper.java"
        LOG.debug("Writing handler wrapper: %s", path)
        template = self.env.get_template("generate/HandlerWrapper.java")
        contents = template.render(
            package_name=self.package_name,
            operations=project.schema.get("handlers", {}).keys(),
            pojo_name="ResourceModel",
        )
        project.overwrite(path, contents)

        path = src / "BaseConfiguration.java"
        LOG.debug("Writing base configuration: %s", path)
        template = self.env.get_template("generate/BaseConfiguration.java")
        contents = template.render(
            package_name=self.package_name,
            schema_file_name=project.schema_filename,
            pojo_name="ResourceModel",
        )
        project.overwrite(path, contents)

        path = src / "BaseHandler.java"
        LOG.debug("Writing base handler: %s", path)
        template = self.env.get_template("generate/BaseHandler.java")
        contents = template.render(
            package_name=self.package_name,
            operations=OPERATIONS,
            pojo_name="ResourceModel",
        )
        project.overwrite(path, contents)

        # generate POJOs
        models = resolve_models(project.schema)

        LOG.debug("Writing %d POJOs", len(models))

        base_template = self.env.get_template("generate/ResourceModel.java")
        pojo_template = self.env.get_template("generate/POJO.java")

        for model_name, properties in models.items():
            path = src / "{}.java".format(model_name)
            LOG.debug("%s POJO: %s", model_name, path)

            if model_name == "ResourceModel":
                contents = base_template.render(
                    type_name=project.type_name,
                    package_name=self.package_name,
                    model_name=model_name,
                    properties=properties,
                    primaryIdentifier=project.schema.get("primaryIdentifier", []),
                    additionalIdentifiers=project.schema.get(
                        "additionalIdentifiers", []
                    ),
                )
            else:
                contents = pojo_template.render(
                    package_name=self.package_name,
                    model_name=model_name,
                    properties=properties,
                )
            project.overwrite(path, contents)

    @staticmethod
    def _find_jar(project):
        jar_glob = list(
            (project.root / "target").glob("{}-*.jar".format(project.hypenated_name))
        )
        if not jar_glob:
            LOG.debug("No Java ARchives match")
            raise JavaArchiveNotFoundError(
                "No JAR artifact was found.\n"
                "Please run 'mvn package' or the equivalent command "
                "in your IDE to compile and package the code."
            )

        if len(jar_glob) > 1:
            LOG.debug(
                "Multiple Java ARchives match: %s",
                ", ".join(str(path) for path in jar_glob),
            )
            raise InternalError("Multiple JARs match")

        return jar_glob[0]

    @logdebug
    def package(self, project, zip_file):
        """Packaging Java project"""

        def write_with_relative_path(path):
            relative = path.relative_to(project.root)
            zip_file.write(path.resolve(), str(relative))

        jar = self._find_jar(project)
        write_with_relative_path(jar)
        write_with_relative_path(project.root / "pom.xml")

        for path in (project.root / "src").rglob("*"):
            if path.is_file():
                write_with_relative_path(path)

        # include these for completeness...
        # we'd probably auto-gen then again, but it can't hurt
        for path in (project.root / "target" / "generated-sources").rglob("*"):
            if path.is_file():
                write_with_relative_path(path)
