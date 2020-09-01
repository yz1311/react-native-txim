import { DeviceEventEmitter, NativeAppEventEmitter, NativeModules, Platform } from 'react-native';

const IMEventEmitter = Platform.select({
    android: NativeAppEventEmitter,
    ios: new NativeEventEmitter(NativeModules.IMInitializeModule),
});


export default IMEventEmitter;