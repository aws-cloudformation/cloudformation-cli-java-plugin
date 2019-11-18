## AWS CloudFormation Resource Provider Java Plugin


The CloudFormation Resource Provider Development Kit (RPDK) allows you to author your own resource providers that can be used by CloudFormation.

This plugin library helps to provide runtime bindings for the execution of your providers by CloudFormation.

Development
-----------

First, you will need to install the (CloudFormation CLI)[https://github.com/aws-cloudformation/aws-cloudformation-rpdk] and install it first, as it is a required dependency:

```
pip install -e ../cloudformation-cli
```

For changes to the plugin, a Python virtual environment is recommended.

```
python3 -m venv env
source env/bin/activate
# assuming aws-cloudformation-rpdk has already been installed
pip -e .
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
