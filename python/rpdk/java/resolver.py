import logging

from rpdk.core.jsonutils.resolver import FORMAT_DEFAULT, UNDEFINED, ContainerType

LOG = logging.getLogger(__name__)

PRIMITIVE_TYPES = {
    "string": {"default": "String"},
    "integer": {"default": "Integer", "int32": "Integer", "int64": "Long"},
    "boolean": {"default": "Boolean"},
    "number": {"default": "Double"},
    UNDEFINED: {"default": "Object"},
}


def translate_type(resolved_type):
    if resolved_type.container == ContainerType.MODEL:
        return resolved_type.type
    if resolved_type.container == ContainerType.PRIMITIVE:
        try:
            primitive_format = PRIMITIVE_TYPES[resolved_type.type][
                resolved_type.type_format
            ]
        except KeyError:
            primitive_format = PRIMITIVE_TYPES[resolved_type.type][FORMAT_DEFAULT]
            LOG.error(
                "Could not find specified format '%s' for type '%s'. "
                "Defaulting to '%s'",
                resolved_type.type_format,
                resolved_type.type,
                primitive_format,
            )
        return primitive_format

    if resolved_type.container == ContainerType.MULTIPLE:
        return "Object"

    item_type = translate_type(resolved_type.type)

    if resolved_type.container == ContainerType.DICT:
        key_type = PRIMITIVE_TYPES["string"]["default"]
        return f"Map<{key_type}, {item_type}>"
    if resolved_type.container == ContainerType.LIST:
        return f"List<{item_type}>"
    if resolved_type.container == ContainerType.SET:
        return f"Set<{item_type}>"

    raise ValueError(f"Unknown container type {resolved_type.container}")
