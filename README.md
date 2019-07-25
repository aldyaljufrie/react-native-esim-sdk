
# react-native-esim-sdk

## Getting started

`$ npm install git+https://github.com/aldyaljufrie/react-native-esim-sdk.git#development --save`

### Mostly automatic installation

`$ react-native link react-native-esim-sdk`

### Manual installation

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNEsimSdkPackage;` to the imports at the top of the file
  - Add `new RNEsimSdkPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-esim-sdk'
  	project(':react-native-esim-sdk').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-esim-sdk/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-esim-sdk')
  	```
## Usage
```javascript
import RNEsimSdk from 'react-native-esim-sdk';

// TODO: What to do with the module?

// init printer SDK
RNEsimSdk.printReceipt(...params);
```
