rm -rf intermediate-builds release template/META-INF/robovm/ios/libs/jme3-ios-native.xcframework
mkdir intermediate-builds release
xcodebuild archive -project jme3-ios-native.xcodeproj -scheme jme3-ios-native -destination generic/platform=iOS -archivePath intermediate-builds/jme3-ios-native_iOS SKIP_INSTALL=NO BUILD_LIBRARY_FOR_DISTRIBUTION=YES
xcodebuild archive -project jme3-ios-native.xcodeproj -scheme jme3-ios-native -destination generic/platform="iOS Simulator" -archivePath intermediate-builds/jme3-ios-native_iOS-Simulator SKIP_INSTALL=NO BUILD_LIBRARY_FOR_DISTRIBUTION=YES

xcodebuild -create-xcframework -framework intermediate-builds/jme3-ios-native_iOS.xcarchive/Products/Library/Frameworks/jme3_ios_native.framework -framework intermediate-builds/jme3-ios-native_iOS-Simulator.xcarchive/Products/Library/Frameworks/jme3_ios_native.framework -output template/META-INF/robovm/ios/libs/jme3-ios-native.xcframework

cd template
zip -r ../release/jme3-ios-native.jar META-INF
cd ..

