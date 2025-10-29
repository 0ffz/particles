# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This project does not fully adhere to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) while under heavy
development.

## [Unreleased]

### Added

- Started a changelog based on keep a changelog
- Mean square velocity calculation via a compute shader that sums log_2 times; plan to extract into a general API in the
  future
- Added more options to graphs displayed in GUI like toggling on/off and clearing
- Added helper function for drawing graphs for values that update over time

### Changed

- Decreased strength of wall repulsion to fix odd behaviour in 3d, as well as sticking to walls under certain force
  parameters