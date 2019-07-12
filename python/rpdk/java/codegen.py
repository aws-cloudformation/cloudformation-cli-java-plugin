# pylint: disable=useless-super-delegation,too-many-locals
# pylint doesn't recognize abstract methods
import logging
import shutil

from rpdk.core.data_loaders import resource_stream
from rpdk.core.exceptions import InternalError, SysExitRecommendedError
from rpdk.core.jsonutils.flattener import JsonSchemaFlattener
from rpdk.core.plugin_base import LanguagePlugin

from .pojo_resolver import JavaPojoResolver
from .utils import safe_reserved

LOG = logging.getLogger(__name__)

OPERATIONS = ("Create", "Read", "Update", "Delete", "List")
EXECUTABLE = "cfn-cli"


class JavaArchiveNotFoundError(SysExitRecommendedError):
    pass


class JavaLanguagePlugin(LanguagePlugin):
    MODULE_NAME = __name__
    RUNTIME = "java8"
    ENTRY_POINT = "{}.HandlerWrapper::handleRequest"
    CODE_URI = "./target/{}-1.0-SNAPSHOT.jar"

    def __init__(self):
        self.env = self._setup_jinja_env(
            trim_blocks=True, lstrip_blocks=True, keep_trailing_newline=True
        )
        self.namespace = None
        self.package_name = None

    def _namespace_from_project(self, project):
        self.namespace = ("com",) + tuple(
            safe_reserved(s.lower()) for s in project.type_info
        )
        self.package_name = ".".join(self.namespace)

    def init(self, project):
        LOG.debug("Init started")

        self._namespace_from_project(project)

        project.runtime = self.RUNTIME
        project.entrypoint = self.ENTRY_POINT.format(self.package_name)

        # .gitignore
        path = project.root / ".gitignore"
        LOG.debug("Writing .gitignore: %s", path)
        contents = resource_stream(__name__, "data/java.gitignore").read()
        project.safewrite(path, contents)

        # maven folder structure
        src = (project.root / "src" / "main" / "java").joinpath(*self.namespace)
        LOG.debug("Making source folder structure: %s", src)
        src.mkdir(parents=True, exist_ok=True)
        tst = (project.root / "src" / "test" / "java").joinpath(*self.namespace)
        LOG.debug("Making test folder structure: %s", tst)
        tst.mkdir(parents=True, exist_ok=True)

        path = project.root / "pom.xml"
        LOG.debug("Writing Maven POM: %s", path)
        template = self.env.get_template("pom.xml")
        artifact_id = "{}-handler".format(project.hypenated_name)
        contents = template.render(
            group_id=self.package_name,
            artifact_id=artifact_id,
            executable=EXECUTABLE,
            schema_file_name=project.schema_filename,
            package_name=self.package_name,
        )
        project.safewrite(path, contents)

        path = project.root / "lombok.config"
        LOG.debug("Writing Lombok Config: %s", path)
        template = self.env.get_template("lombok.config")
        contents = template.render()
        project.safewrite(path, contents)

        # CloudFormation/SAM template for handler lambda
        path = project.root / "template.yml"
        LOG.debug("Writing SAM template: %s", path)
        template = self.env.get_template("template.yml")

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
                    "Handler": handler_params["Handler"].replace(
                        "handleRequest", "testEntrypoint"
                    ),
                },
            },
        )
        project.safewrite(path, contents)

        LOG.debug("Writing handlers and tests")
        self.init_handlers(project, src, tst)

        LOG.debug("Writing callback context")
        template = self.env.get_template("CallbackContext.java")
        path = src / "CallbackContext.java"
        contents = template.render(package_name=self.package_name)
        project.safewrite(path, contents)

        path = src / "Configuration.java"
        LOG.debug("Writing configuration: %s", path)
        template = self.env.get_template("StubConfiguration.java")
        contents = template.render(
            package_name=self.package_name, schema_file_name=project.schema_filename
        )
        project.safewrite(path, contents)

        path = project.root / "README.md"
        LOG.debug("Writing README: %s", path)
        template = self.env.get_template("README.md")
        contents = template.render(
            type_name=project.type_name,
            schema_path=project.schema_path,
            executable=EXECUTABLE,
        )
        project.safewrite(path, contents)

        LOG.debug("Init complete")

    def init_handlers(self, project, src, tst):
        LOG.debug("Writing stub handlers")
        for operation in OPERATIONS:
            if operation == "List":
                template = self.env.get_template("StubListHandler.java")
            else:
                template = self.env.get_template("StubHandler.java")
            path = src / "{}Handler.java".format(operation)
            LOG.debug("%s handler: %s", operation, path)
            contents = template.render(
                package_name=self.package_name,
                operation=operation,
                pojo_name="ResourceModel",
            )
            project.safewrite(path, contents)

        LOG.debug("Writing stub tests")
        for operation in OPERATIONS:
            if operation == "List":
                template = self.env.get_template("StubListHandlerTest.java")
            else:
                template = self.env.get_template("StubHandlerTest.java")

            path = tst / "{}HandlerTest.java".format(operation)
            LOG.debug("%s handler: %s", operation, path)
            contents = template.render(
                package_name=self.package_name,
                operation=operation,
                pojo_name="ResourceModel",
            )
            project.safewrite(path, contents)

    @staticmethod
    def _get_generated_root(project):
        return project.root / "target" / "generated-sources" / "rpdk"

    @staticmethod
    def _get_generated_tests_root(project):
        return project.root / "target" / "generated-test-sources" / "rpdk"

    def generate(self, project):
        LOG.debug("Generate started")

        self._namespace_from_project(project)

        objects = JsonSchemaFlattener(project.schema).flatten_schema()

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
        template = self.env.get_template("HandlerWrapper.java")
        contents = template.render(
            package_name=self.package_name,
            operations=OPERATIONS,
            pojo_name="ResourceModel",
        )
        project.overwrite(path, contents)

        path = src / "BaseConfiguration.java"
        LOG.debug("Writing base configuration: %s", path)
        template = self.env.get_template("BaseConfiguration.java")
        contents = template.render(
            package_name=self.package_name, schema_file_name=project.schema_filename
        )
        project.overwrite(path, contents)

        path = src / "BaseHandler.java"
        LOG.debug("Writing base handler: %s", path)
        template = self.env.get_template("BaseHandler.java")
        contents = template.render(
            package_name=self.package_name,
            operations=OPERATIONS,
            pojo_name="ResourceModel",
        )
        project.overwrite(path, contents)

        # generate pojos
        pojo_resolver = JavaPojoResolver(objects, "ResourceModel")
        pojos = pojo_resolver.resolve_pojos()

        LOG.debug("Writing %d POJOs", len(pojos))

        template = self.env.get_template("POJO.java")
        for pojo_name, properties in pojos.items():
            path = src / "{}.java".format(pojo_name)
            LOG.debug("%s POJO: %s", pojo_name, path)
            contents = template.render(
                type_name=project.type_name,
                package_name=self.package_name,
                pojo_name=pojo_name,
                properties=properties,
                primaryIdentifier=project.schema.get("primaryIdentifier", []),
                additionalIdentifiers=project.schema.get("additionalIdentifiers", []),
            )
            project.overwrite(path, contents)

        # generate pojo tests
        template = self.env.get_template("ResourceModelTest.java")

        path = test_src / "ResourceModelTest.java"
        LOG.debug("Writing ResourceModelTest: %s", path)
        contents = template.render(
            type_name=project.type_name, package_name=self.package_name
        )
        project.overwrite(path, contents)

        LOG.debug("Generate complete")

    @staticmethod
    def _find_jar(project):
        jar_glob = list(
            (project.root / "target").glob(
                "{}-*-SNAPSHOT.jar".format(project.hypenated_name)
            )
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

    def package(self, project, zip_file):
        LOG.info("Packaging Java project")

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
