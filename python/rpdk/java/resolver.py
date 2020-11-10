from rpdk.core.jsonutils.resolver import UNDEFINED, ContainerType

PRIMITIVE_TYPES = {
    "string": {"default": "String"},
    "integer": {
                "default": "Integer",
                "int32": "Integer",
                "int64": "Integer"
                },
    "boolean": {"default": "Boolean"},
    "number":  {"default": "Double"},
    UNDEFINED: {"default": "Object"}
}


def translate_type(resolved_type):
    if resolved_type.container == ContainerType.MODEL:
        return resolved_type.type
    if resolved_type.container == ContainerType.PRIMITIVE:
        return PRIMITIVE_TYPES[resolved_type.type].get(resolved_type.format)

    if resolved_type.container == ContainerType.MULTIPLE:
        return "Object"

    item_type = translate_type(resolved_type.type)

    if resolved_type.container == ContainerType.DICT:
        key_type = PRIMITIVE_TYPES["string"]
        return f"Map<{key_type}, {item_type}>"
    if resolved_type.container == ContainerType.LIST:
        return f"List<{item_type}>"
    if resolved_type.container == ContainerType.SET:
        return f"Set<{item_type}>"

    raise ValueError(f"Unknown container type {resolved_type.container}")
