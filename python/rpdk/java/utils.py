import re
import string

from rpdk.core.exceptions import WizardValidationError

# https://docs.oracle.com/javase/tutorial/java/nutsandbolts/_keywords.html
LANGUAGE_KEYWORDS = {
    "abstract",
    "continue",
    "for",
    "new",
    "switch",
    "assert",
    "default",
    "goto",
    "package",
    "synchronized",
    "boolean",
    "do",
    "if",
    "private",
    "this",
    "break",
    "double",
    "implements",
    "protected",
    "throw",
    "byte",
    "else",
    "import",
    "public",
    "throws",
    "case",
    "enum",
    "instanceof",
    "return",
    "transient",
    "catch",
    "extends",
    "int",
    "short",
    "try",
    "char",
    "final",
    "interface",
    "static",
    "void",
    "class",
    "finally",
    "long",
    "strictfp",
    "volatile",
    "const",
    "float",
    "native",
    "super",
    "while",
}


def safe_reserved(token):
    if token in LANGUAGE_KEYWORDS:
        return token + "_"
    return token


def validate_namespace(default):
    pattern = r"^[_a-z][_a-z0-9]+$"

    def _validate_namespace(value):
        if not value:
            return default

        if value.lower() != value:
            raise WizardValidationError("Package names must be all lower case")

        namespace = value.split(".")

        for name in namespace:
            if not name:
                raise WizardValidationError(f"Empty segment in '{value}'")
            if name in LANGUAGE_KEYWORDS:
                raise WizardValidationError(f"'{name}' is a reserved keyword")
            startswith = name[0]
            if startswith not in string.ascii_lowercase + "_":
                raise WizardValidationError(
                    f"Segment '{name}' must begin with a lower case letter or "
                    "an underscore"
                )

            match = re.match(pattern, name)
            if not match:
                raise WizardValidationError(
                    f"Segment '{name}' should match '{pattern}'"
                )

        return tuple(namespace)

    return _validate_namespace


def validate_codegen_model(default):
    pattern = r"^[1-2]$"

    def _validate_codegen_model(value):
        if not value:
            return default

        match = re.match(pattern, value)
        if not match:
            raise WizardValidationError("Invalid selection.")

        return value

    return _validate_codegen_model
