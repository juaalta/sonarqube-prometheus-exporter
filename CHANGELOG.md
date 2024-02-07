# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

* Process more than 500 SonarQube projects by [marceloavilaoliveira](https://github.com/marceloavilaoliveira). Pull Request: [PR1](https://github.com/juaalta/sonarqube-prometheus-exporter/pull/1)

### Changed

### Deprecated

### Removed

### Fixed

### Security


- - -

## 1.1.0 - 2022-01-03

### Added

* The metrics are obtained from the list of metrics that the SonarQube has internally.
* Deprecated metrics are removed.
* Metrics without description are removed, since when the output is generated for Prometheus the process fails as it does not take into account that the description can be empty
* Metrics of numeric type return their value directly.
* Metrics of type LEVEL return 0 as a result and with a label called level, the value of the level of the metric.
* The STRING type metrics return a 0 as a result and with a label called value, the value of the metric.
* Added docker composition file to be able to test plugin. 
* Added configuration files for the correct operation of Prometheus and Grafana.
* Moved the example dashboard within the Grafana folders, to be able to see how it works.
* Added a test.
* Added Jacoco for code coverage.

### Changed

* Moved to the images folder, to be better organized files.
* Updated Grafana images.
* Changed TravisCI for Github Workflows

