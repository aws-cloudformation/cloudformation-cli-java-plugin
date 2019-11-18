## AWS CloudFormation Resource Provider Java Plugin

The CloudFormation CLI (cfn) allows you to author your own resource providers that can be used by CloudFormation.

This plugin library helps to provide Java runtime bindings for the execution of your providers by CloudFormation.

Usage
-----

If you are using this package to build resource providers for CloudFormation, install the (CloudFormation CLI)[https://github.com/aws-cloudformation/aws-cloudformation-rpdk] and the (CloudFormation CLI Java Plugin)[https://github.com/aws-cloudformation/aws-cloudformation-rpdk-java-plugin]

```
pip install cloudformation-cli
pip install cloudformation-cli-java-plugin
```

Refer to the documentation for the [CloudFormation CLI](https://github.com/aws-cloudformation/aws-cloudformation-rpdk) for usage instructions.

Development
-----------

First, you will need to install the (CloudFormation CLI)[https://github.com/aws-cloudformation/aws-cloudformation-rpdk], as it is a required dependency:

```
pip install cloudformation-cli
```

For changes to the plugin, a Python virtual environment is recommended.

```
python3 -m venv env
source env/bin/activate
# assuming cloudformation-cli has already been cloned/downloaded
pip install -e .
pre-commit install
```

Linting and running unit tests is done via [pre-commit](https://pre-commit.com/), and so is performed automatically on commit. The continuous integration also runs these checks. Manual options are available so you don't have to commit):

```
# run all hooks on all files, mirrors what the CI runs
pre-commit run --all-files
# run unit tests only. can also be used for other hooks, e.g. black, flake8, pylint-local
pre-commit run pytest-local
```

License
-------

This library is licensed under the Apache 2.0 License.
