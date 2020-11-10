import pytest
from rpdk.core.jsonutils.resolver import MULTIPLE, ContainerType, ResolvedType
from rpdk.java.resolver import PRIMITIVE_TYPES, translate_type

RESOLVED_TYPES = [
    (ResolvedType(ContainerType.PRIMITIVE, item_type), formats["default"])
    for item_type, formats in PRIMITIVE_TYPES.items()
]

RESOLVED_INTEGER_FORMATS = [
    (
        ResolvedType(ContainerType.PRIMITIVE, "integer", "int64"),
        PRIMITIVE_TYPES["integer"]["int64"],
    ),
    (
        ResolvedType(ContainerType.PRIMITIVE, "integer", "int32"),
        PRIMITIVE_TYPES["integer"]["int32"],
    ),
]


def test_translate_type_model_passthrough():
    item_type = object()
    traslated = translate_type(ResolvedType(ContainerType.MODEL, item_type))
    assert traslated is item_type


@pytest.mark.parametrize("resolved_type,native_type", RESOLVED_TYPES)
def test_translate_type_primitive(resolved_type, native_type):
    assert translate_type(resolved_type) == native_type


@pytest.mark.parametrize("resolved_type,native_type", RESOLVED_TYPES)
def test_translate_type_dict(resolved_type, native_type):
    traslated = translate_type(ResolvedType(ContainerType.DICT, resolved_type))
    assert traslated == f"Map<String, {native_type}>"


def test_translate_type_multiple():
    assert translate_type(ResolvedType(ContainerType.MULTIPLE, MULTIPLE)) == "Object"


@pytest.mark.parametrize("resolved_type,native_type", RESOLVED_TYPES)
def test_translate_type_list(resolved_type, native_type):
    traslated = translate_type(ResolvedType(ContainerType.LIST, resolved_type))
    assert traslated == f"List<{native_type}>"


@pytest.mark.parametrize("resolved_type,native_type", RESOLVED_TYPES)
def test_translate_type_set(resolved_type, native_type):
    traslated = translate_type(ResolvedType(ContainerType.SET, resolved_type))
    assert traslated == f"Set<{native_type}>"


@pytest.mark.parametrize("resolved_type,_java_type", RESOLVED_TYPES)
def test_translate_type_unknown(resolved_type, _java_type):
    with pytest.raises(ValueError):
        translate_type(ResolvedType("foo", resolved_type))


@pytest.mark.parametrize("resolved_type,java_type", RESOLVED_INTEGER_FORMATS)
def test_translate_type_integer_formats(resolved_type, java_type):
    assert translate_type(resolved_type) == java_type


def test_translate_type_unavailable_format():
    resolved_type = ResolvedType(ContainerType.PRIMITIVE, "integer", "int128")
    assert translate_type(resolved_type) == PRIMITIVE_TYPES["integer"]["default"]
