# AMQ 7 Reference Architecture S2I Base Image

AMQ7 RefArch project showcasing various broker uses, clustering, high-availability & interconnect routing features.

### Prerequisites

OpenShift 3.X environment

### Installing

Create a new project on OpenShift:

```
oc new-project amq
```

Import base AMQ7 S2I image template:

```
oc process -f https://raw.githubusercontent.com/jeremyary/amq7-image/master/yaml_templates/amq_image_template.yaml | oc create -f -
```

Trigger new build of base image:

```
oc start-build amq7-image
```

Import single, symmetric, replicated, and interconnect templates:

```
oc process -f https://raw.githubusercontent.com/jeremyary/amq7-image/master/yaml_templates/amq_single_template.yaml | oc create -f -
oc process -f https://raw.githubusercontent.com/jeremyary/amq7-image/master/yaml_templates/amq_symmetric_template.yaml | oc create -f -
oc process -f https://raw.githubusercontent.com/jeremyary/amq7-image/master/yaml_templates/amq_replicated_template.yaml | oc create -f -
oc process -f https://raw.githubusercontent.com/jeremyary/amq7-image/master/yaml_templates/amq_interconnect_template.yaml | oc create -f -
```

### Correlating Test Project Repo

Project containing a suite of tests that exercise the various populated services/cluster:

https://github.com/jeremyary/amq7-refarch