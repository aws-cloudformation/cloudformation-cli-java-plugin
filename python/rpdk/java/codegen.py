# pylint: disable=useless-super-delegation,too-many-locals
# pylint doesn't recognize abstract methods
import logging
import os
import shutil
import xml.etree.ElementTree as ET  # nosec
from collections import namedtuple
from xml.etree.ElementTree import ParseError  # nosec

from rpdk.core.data_loaders import resource_stream
from rpdk.core.exceptions import InternalError, SysExitRecommendedError
from rpdk.core.init import input_with_validation
from rpdk.core.jsonutils.resolver import resolve_models
from rpdk.core.plugin_base import LanguagePlugin

from . import __version__
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


DEFAULT_PROTOCOL_VERSION = "2.0.0"
PROTOCOL_VERSION_SETTING = "protocolVersion"
DEFAULT_SETTINGS = {PROTOCOL_VERSION_SETTING: DEFAULT_PROTOCOL_VERSION}

MINIMUM_JAVA_DEPENDENCY_VERSION = "2.0.0"
MINIMUM_JAVA_DEPENDENCY_VERSION_EXECUTABLE_HANDLER_WRAPPER = "2.0.3"


class JavaArchiveNotFoundError(SysExitRecommendedError):
    pass


class JavaPluginVersionNotSupportedError(SysExitRecommendedError):
    pass


class JavaPluginNotFoundError(SysExitRecommendedError):
    pass


class InvalidMavenPOMError(SysExitRecommendedError):
    pass


class JavaLanguagePlugin(LanguagePlugin):
    MODULE_NAME = __name__
    RUNTIME = "java8"
    ENTRY_POINT = "{}.HandlerWrapper::handleRequest"
    TEST_ENTRY_POINT = "{}.HandlerWrapper::testEntrypoint"
    EXECUTABLE_ENTRY_POINT = "{}.HandlerWrapperExecutable"
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
            project.write_settings()

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
        resources = project.root / "src" / "resources"
        LOG.debug("Making resources folder structure: %s", resources)
        resources.mkdir(parents=True, exist_ok=True)
        tst = (project.root / "src" / "test" / "java").joinpath(*self.namespace)
        LOG.debug("Making test folder structure: %s", tst)
        tst.mkdir(parents=True, exist_ok=True)

        # initialize shared files
        self.init_shared(project, src, tst, resources)

        # write specialized generated files
        if self._is_aws_guided(project):
            self.init_guided_aws(project, src, tst)

    @logdebug
    def init_shared(self, project, src, tst, resources):
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

        # log4j2
        path = resources / "log4j2.xml"
        LOG.debug("Writing log4j2: %s", path)
        contents = resource_stream(__name__, "data/log4j2.xml").read()
        project.safewrite(path, contents)

        self.init_handlers(project, src, tst)

    @logdebug
    def init_guided_aws(self, project, src, tst):
        """Writing supporting modules"""
        self._writing_component(project, src, entity="Translator.java")
        self._writing_component(project, src, entity="ClientBuilder.java")
        self._writing_component(project, src, entity="BaseHandlerStd.java")
        self._writing_component(
            project,
            src,
            entity="TagHelper.java",
            operation="TagOps",
            call_graph=project.type_name.replace("::", "-"),
        )
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
        project.executable_entrypoint = self.EXECUTABLE_ENTRY_POINT.format(
            self.package_name
        )
        project.settings.update(DEFAULT_SETTINGS)

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
            contains_type_configuration=project.configuration_schema,
            pojo_name="ResourceModel",
            wrapper_parent="LambdaWrapper",
        )
        project.overwrite(path, contents)

        # write generated handler integration with ExecutableWrapper
        self._write_executable_wrapper_class(src, project)

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
            contains_type_configuration=project.configuration_schema,
            pojo_name="ResourceModel",
        )
        project.overwrite(path, contents)

        # generate POJOs
        models = resolve_models(project.schema)
        if project.configuration_schema:
            configuration_schema_path = (
                self._get_generated_root(project)
                / project.configuration_schema_filename
            )
            project.write_configuration_schema(configuration_schema_path)
            configuration_models = resolve_models(
                project.configuration_schema, "TypeConfigurationModel"
            )
        else:
            configuration_models = {"TypeConfigurationModel": {}}
        models.update(configuration_models)

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
                    no_args_constructor_required=(
                        model_name != "TypeConfigurationModel" or len(properties) != 0
                    ),
                )
            project.overwrite(path, contents)

        self._update_settings(project)

        LOG.debug("Generate complete")

    def _write_executable_wrapper_class(self, src, project):
        try:
            java_plugin_dependency_version = self._get_java_plugin_dependency_version(
                project
            )
            if (
                java_plugin_dependency_version
                >= MINIMUM_JAVA_DEPENDENCY_VERSION_EXECUTABLE_HANDLER_WRAPPER
            ):
                path = src / "HandlerWrapperExecutable.java"
                LOG.debug("Writing handler wrapper: %s", path)
                template = self.env.get_template("generate/HandlerWrapper.java")
                contents = template.render(
                    package_name=self.package_name,
                    operations=project.schema.get("handlers", {}).keys(),
                    pojo_name="ResourceModel",
                    contains_type_configuration=project.configuration_schema,
                    wrapper_parent="ExecutableWrapper",
                )
                project.overwrite(path, contents)
            else:
                LOG.info(
                    "Please update your java plugin dependency to version "
                    "%s or above in order to use "
                    "the Executable Handler Wrapper feature.",
                    MINIMUM_JAVA_DEPENDENCY_VERSION_EXECUTABLE_HANDLER_WRAPPER,
                )
        except JavaPluginNotFoundError:
            LOG.info(
                "Please make sure to have 'aws-cloudformation-rpdk-java-plugin' "
                "to version %s or above.",
                MINIMUM_JAVA_DEPENDENCY_VERSION,
            )

    def _update_settings(self, project):
        try:
            java_plugin_dependency_version = self._get_java_plugin_dependency_version(
                project
            )
            if java_plugin_dependency_version < MINIMUM_JAVA_DEPENDENCY_VERSION:
                raise JavaPluginVersionNotSupportedError(
                    "'aws-cloudformation-rpdk-java-plugin' {} is no longer supported."
                    "Please update it in pom.xml to version {} or above.".format(
                        java_plugin_dependency_version, MINIMUM_JAVA_DEPENDENCY_VERSION
                    )
                )
        except JavaPluginNotFoundError:
            LOG.info(
                "Please make sure to have 'aws-cloudformation-rpdk-java-plugin' "
                "to version %s or above.",
                MINIMUM_JAVA_DEPENDENCY_VERSION,
            )

        protocol_version = project.settings.get(PROTOCOL_VERSION_SETTING)
        if protocol_version != DEFAULT_PROTOCOL_VERSION:
            project.settings[PROTOCOL_VERSION_SETTING] = DEFAULT_PROTOCOL_VERSION
            project.write_settings()

        if (
            hasattr(project, "executable_entrypoint")
            and not project.executable_entrypoint
        ):
            project.executable_entrypoint = self.EXECUTABLE_ENTRY_POINT.format(
                self.package_name
            )
            project.write_settings()

    @staticmethod
    def _find_jar(project):
        jar_glob = list(
            (project.root / "target").glob("{}-*.jar".format(project.hypenated_name))
        )
        if not jar_glob:
            LOG.debug("No Java Archives matched at %s", str(project.root / "target"))
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

    @staticmethod
    def _get_java_plugin_dependency_version(project):
        try:
            tree = ET.parse(project.root / "pom.xml")
            root = tree.getroot()
            namespace = {"mvn": "http://maven.apache.org/POM/4.0.0"}
            plugin_dependency_version = root.find(
                "./mvn:dependencies/mvn:dependency"
                "/[mvn:artifactId='aws-cloudformation-rpdk-java-plugin']/mvn:version",
                namespace,
            )
            if plugin_dependency_version is None:
                raise JavaPluginNotFoundError(
                    "'aws-cloudformation-rpdk-java-plugin' maven dependency "
                    "not found in pom.xml."
                )
            return plugin_dependency_version.text
        except ParseError as e:
            raise InvalidMavenPOMError("pom.xml is invalid.") from e

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

    @logdebug
    def generate_image_build_config(self, project):
        """Generating image build config"""

        jar_path = self._find_jar(project)

        dockerfile_path = (
            os.path.dirname(os.path.realpath(__file__))
            + "/data/build-image-src/Dockerfile-"
            + project.runtime
        )

        project_path = project.root

        return {
            "executable_name": str(jar_path.relative_to(project.root)),
            "dockerfile_path": dockerfile_path,
            "project_path": str(project_path),
        }

    @staticmethod
    def _get_plugin_information(project):
        return {
            "plugin-tool-version": __version__,
            "plugin-name": "java",
            "plugin-version": JavaLanguagePlugin._get_java_plugin_dependency_version(
                project
            ),
        }

    @logdebug
    def get_plugin_information(self, project):
        return self._get_plugin_information(project)
