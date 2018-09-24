# BasicCamera
This Android project is an example for a framework using the android.hardware.camera package.
The framework offers the possibility to capture images with the camera or take preview frames,
to then process the image in its own thread, to finally draw them on the camera surface.
As example for a process I use a Canny edge detector.

# Dependencies
To run the project the OpenCV library for Android is required.

### Instructions for Android Studio

1. Import the project "New -> Import Project..." and choose the "app" folder.
2. Download the OpenCV library for Android http://opencv.org/downloads.html and extract the folder somewhere.
3. In Android studio go to "File -> Project Structure" and click on "New Module" (the green plus in the top left corner); a new window opens; click on "Import Existing Project", the source directory is "YOUR OPENCV DIRECTORY/sdk/java", then click on next two times.

# Software architecture
UML diagrams can be found in the docs folder. As example the class diagram:

# References
The project ist based on the QR code scanner app https://github.com/zxing
