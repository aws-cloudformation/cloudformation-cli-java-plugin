## AWS CloudFormation Resource Provider Java Plugin


The CloudFormation Resource Provider Development Kit (RPDK) allows you to author your own resource providers that can be used by CloudFormation.

This plugin library helps to provide runtime bindings for the execution of your providers by CloudFormation.

Development
-----------

For changes to the plugin, a Python virtual environment is recommended. You also need to download `aws-cloudformation-rpdk` and install it first, as it isn't currently available on PyPI, but is a required dependency:

```
python3 -m venv env
source env/bin/activate
# assuming aws-cloudformation-rpdk has already been cloned/downloaded
pip install \
    -e ../aws-cloudformation-rpdk \
    -r ../aws-cloudformation-rpdk/requirements.txt \
    -e .
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
