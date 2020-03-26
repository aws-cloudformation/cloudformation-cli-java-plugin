# fixture and parameter have the same name
# pylint: disable=redefined-outer-name
import pytest
from rpdk.core.exceptions import WizardValidationError
from rpdk.java.utils import (
    safe_reserved,
    validate_codegen_model as validate_codegen_model_factory,
    validate_namespace as validate_namespace_factory,
)

DEFAULT = object()


@pytest.fixture
def validate_namespace():
    return validate_namespace_factory(DEFAULT)


@pytest.fixture
def validate_codegen_model():
    return validate_codegen_model_factory(DEFAULT)


def test_safe_reserved_safe_string():
    assert safe_reserved("foo") == "foo"


def test_safe_reserved_unsafe_string():
    assert safe_reserved("class") == "class_"


def test_validate_namespace_empty(validate_namespace):
    assert validate_namespace("") == DEFAULT


def test_validate_namespace_upper(validate_namespace):
    with pytest.raises(WizardValidationError) as excinfo:
        validate_namespace("com.foo.Bar")
    assert "lower" in str(excinfo.value)


def test_validate_namespace_segment_empty(validate_namespace):
    value = "com.foo..bar"
    with pytest.raises(WizardValidationError) as excinfo:
        validate_namespace(value)
    assert value in str(excinfo.value)


def test_validate_namespace_segment_reserved(validate_namespace):
    with pytest.raises(WizardValidationError) as excinfo:
        validate_namespace("com.foo.final.bar")
    assert "final" in str(excinfo.value)


def test_validate_namespace_segment_begins_numeric(validate_namespace):
    with pytest.raises(WizardValidationError) as excinfo:
        validate_namespace("com.foo.1bar")
    assert "1bar" in str(excinfo.value)

    # this is allowed, and a common workaround, so also check it
    assert validate_namespace("com.foo._1bar") == ("com", "foo", "_1bar")


def test_validate_namespace_segment_hypens(validate_namespace):
    with pytest.raises(WizardValidationError) as excinfo:
        validate_namespace("com.foo-bar")
    assert "foo-bar" in str(excinfo.value)


def test_validate_codegen_model_choose_1(validate_codegen_model):
    assert validate_codegen_model("1") == "1"


def test_validate_codegen_model_choose_2(validate_codegen_model):
    assert validate_codegen_model("2") == "2"


def test_validate_codegen_model_invalid_selection(validate_codegen_model):
    with pytest.raises(WizardValidationError) as excinfo:
        validate_codegen_model("3")
    assert "Invalid selection." in str(excinfo.value)


def test_validate_codegen_model_no_selection(validate_codegen_model):
    assert validate_codegen_model("") == DEFAULT
