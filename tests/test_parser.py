import argparse

from rpdk.java.parser import setup_subparser


def test_setup_subparser():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="subparser_name")

    sub_parser = setup_subparser(subparsers, [])

    args = sub_parser.parse_args(["--namespace", "com.foo.bar", "-c", "default"])

    assert args.language == "java"
    assert args.namespace == "com.foo.bar"
    assert args.codegen_model == "default"
