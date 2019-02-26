## FXMolViewer

*FXMolViewer* is a small project with the original purpose to check, whether JavaFX and its 3D-API may serve as a robust technology to display and manipulate molecules and protein structures interactively in 3D. A particular focus is put on triangle meshes to display opaque and translucent molecular surfaces, which are colored to indicate surface region specific molecular properties or atom types.


### Features

* Interactive small molecule/protein viewer supporting wire, ball, stick, ball&stick modes
* PDB-entries can be downloaded, protein surfaces generated and binding site cropped in one step
* An *Improved Marching Cubes* algorithm creates fast and smooth molecular surfaces
* Visible molecules can be energy-minimized using a MMFF94s+ forcefield
* Entire scene or individual molecules (Ctrl) can be rotated/translated with right/middle mouse button
* Atoms can be selected with lasso selection (left mouse button)
* Front and rear clipping planes are accessible on popup with right mouse click
* Surfaces can be colored by atomic no, polarity, donor/acceptor potential
* Surfaces can be cut smoothly by clipping planes or lasso selection
* A side panel allows to show and hide individual molecules
* The open source ray-tracer SunFlow was built in to render scenes and molecules in high quality


### Dependencies

All dependencies are part of this project and can be found in the ./lib folder:
* OpenChemLib: Cheminformatics base functionality to handle molecules and generate conformers
* SunFlow source code and janino.jar: Ray-Tracer to build photo-realistic images of 3-dimensional scenes
* mmtf-all: Java library to download and parse binary structure files from the PDB-database
* controlsfx-8.40.14.jar: Controls extending JavaFX functionality, used for the molecule side panel


### How to download the project

git clone https://github.com/thsa/fxmolviewer.git


### How to build the project

On Linux or Macintosh just run the 'buildAll' shell script.


### How to run the project

After building it double click fxmolviewer.jar or type in a shell: 'java -jar fxmolviewer.jar'. **Note that this project requires Java 8.** For Java 9 and above you need to replace controlsfx-8.40.14.jar by a newer version.


### How to contribute

Contact the author under the e-mail shown on www.openmolecules.org


### License

FXMolViewer, showing and manipulating molecules and protein structures in 3D.
Copyright (C) 2019 Thomas Sander

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
