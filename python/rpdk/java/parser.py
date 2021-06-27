def setup_subparser(subparsers, parents):
    parser = subparsers.add_parser(
        "java",
        description="This sub command generates IDE and build files for Java",
        parents=parents,
    )
    parser.set_defaults(language="java")

    parser.add_argument(
        "-n",
        "--namespace",
        nargs="?",
        const="default",
        help="""Select the name of the Java namespace.
            Passing the flag without argument select the default namespace.""",
    )

    parser.add_argument(
        "-c",
        "--codegen-model",
        choices=["default", "guided_aws"],
        help="Select a codegen model.",
    )

    return parser
