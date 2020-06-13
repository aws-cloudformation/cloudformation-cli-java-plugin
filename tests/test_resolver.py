import pytest
from rpdk.core.jsonutils.resolver import ContainerType, ResolvedType
from rpdk.java.resolver import MULTIPLE_TYPES, PRIMITIVE_TYPES, translate_type

RESOLVED_TYPES = [
    (ResolvedType(ContainerType.PRIMITIVE, item_type), native_type)
    for item_type, native_type in PRIMITIVE_TYPES.items()
]

RESOLVED_MULTIPLE_TYPES = [
    (ResolvedType(ContainerType.MULTIPLE, item_type), native_type)
    for item_type, native_type in MULTIPLE_TYPES.items()
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


@pytest.mark.parametrize("resolved_type,native_type", RESOLVED_MULTIPLE_TYPES)
def test_translate_type_multiple(resolved_type, native_type):
    assert translate_type(resolved_type) == native_type


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
