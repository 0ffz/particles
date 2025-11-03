# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This project does not fully adhere to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) while under heavy
development.

## [0.4.0] - 2025-11-02

### Added

- Started a changelog based on keep a changelog
- **Mean square velocity** calculation via a compute shader that sums log_2 times; plan to extract into a general API in
  the future
- **Support for recent projects in web** via local storage, plus "Save as" button to save projects back to disk
- **Target velocity** via rescaling based on mean square velocity
- Added more options to graphs displayed in GUI like toggling on/off and clearing
- Added helper function for drawing graphs for values that update over time
- Added banner and icon for the project
- Grid toggle button under visual options

### Changed

- Change project name references from 'Particles' to 'Particle HIVE'
- Decreased strength of wall repulsion to fix odd behaviour in 3d, as well as sticking to walls under certain force
  parameters
- Docs moved to a static site generator we have more control over

### Fixed

- Prevent z-fighting in 2d scenes by rotating billboards upwards by a tiny angle