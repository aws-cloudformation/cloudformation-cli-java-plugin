# fixture and parameter have the same name
# pylint: disable=redefined-outer-name,protected-access
import xml.etree.ElementTree as ET
from unittest.mock import Mock, patch

import pytest
import yaml

from rpdk.core.exceptions import InternalError, SysExitRecommendedError
from rpdk.core.project import Project
from rpdk.java.codegen import JavaArchiveNotFoundError, JavaLanguagePlugin

RESOURCE = "DZQWCC"


@pytest.fixture
def project(tmpdir):
    project = Project(root=tmpdir)
    with patch.dict(
        "rpdk.core.plugin_registry.PLUGIN_REGISTRY",
        {"test": lambda: JavaLanguagePlugin},
        clear=True,
    ):
        project.init("AWS::Foo::{}".format(RESOURCE), "test")
    return project


def test_java_language_plugin_module_is_set():
    plugin = JavaLanguagePlugin()
    assert plugin.MODULE_NAME


def test_initialize(project):
    assert (project.root / "README.md").is_file()

    pom_tree = ET.parse(str(project.root / "pom.xml"))
    namespace = {"maven": "http://maven.apache.org/POM/4.0.0"}
    actual_group_id = pom_tree.find("maven:groupId", namespace)
    expected_group_id = "com.aws.foo.{}".format(RESOURCE.lower())
    assert actual_group_id.text == expected_group_id
    path = project.root / "Handler.yaml"
    with path.open("r", encoding="utf-8") as f:
        template = yaml.safe_load(f)

    handler_properties = template["Resources"]["ResourceHandler"]["Properties"]

    code_uri = "./target/{}-handler-1.0-SNAPSHOT.jar".format(project.hypenated_name)
    assert handler_properties["CodeUri"] == code_uri
    handler = "{}.HandlerWrapper::handleRequest".format(expected_group_id)
    assert handler_properties["Handler"] == handler
    assert handler_properties["Runtime"] == project._plugin.RUNTIME


def test_generate(project):
    project.load_schema()

    generated_root = project._plugin._get_generated_root(project)

    # generated root shouldn't be present
    assert not generated_root.is_dir()

    project.generate()

    test_file = generated_root / "test"
    test_file.touch()

    project.generate()

    # asserts we remove existing files in the tree
    assert not test_file.is_file()


def make_target(project, count):
    target = project.root / "target"
    target.mkdir(exist_ok=True)
    jar_paths = []
    for i in range(count):
        jar_path = target / "{}-{}.0-SNAPSHOT.jar".format(project.hypenated_name, i)
        jar_path.touch()
        jar_paths.append(jar_path)
    return jar_paths


def test__find_jar_zero(project):
    make_target(project, 0)
    with pytest.raises(JavaArchiveNotFoundError) as excinfo:
        project._plugin._find_jar(project)

    assert isinstance(excinfo.value, SysExitRecommendedError)


def test__find_jar_one(project):
    jar_path, *_ = make_target(project, 1)
    assert project._plugin._find_jar(project) == jar_path


def test__find_jar_two(project):
    make_target(project, 2)
    with pytest.raises(InternalError):
        project._plugin._find_jar(project)


def test__get_primary_identifier_none(project):
    assert [] == project._plugin._get_primary_identifier({})


def test__get_additional_identifiers_none(project):
    assert [] == project._plugin._get_additional_identifiers({})


def test_package(project):
    project.load_schema()
    project.generate()
    make_target(project, 1)

    zip_file = Mock()
    project._plugin.package(project, zip_file)

    writes = []
    for call in zip_file.write.call_args_list:
        args, _kwargs = call
        writes.append(str(args[1]))  # relative path

    assert len(writes) > 10
    assert "pom.xml" in writes
