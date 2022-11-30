package land.fx.fula;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.module.annotations.ReactModule;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import fulamobile.Client;
import fulamobile.Fulamobile;
import fulamobile.Config;

@ReactModule(name = FulaModule.NAME)
public class FulaModule extends ReactContextBaseJavaModule {
    public static final String NAME = "FulaModule";
    Client fula;
    String appDir;
    String fulaStorePath;

    public FulaModule(ReactApplicationContext reactContext) {
        super(reactContext);
        appDir = reactContext.getFilesDir().toString();
        fulaStorePath = appDir + "/fula";
        File storeDir = new File(fulaStorePath);
        boolean success = true;
        if (!storeDir.exists()) {
            success = storeDir.mkdirs();
        }
        if(success){
            Log.d(NAME,"Fula store folder created");
        }else{
            Log.d(NAME,"Unable to create fula store folder!");
        }
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }


    private byte[] toByte(String input) {
      return input.getBytes(StandardCharsets.UTF_8);
    }

  private String toString(byte[] input) {
    return new String(input, StandardCharsets.UTF_8);
  }

  private static int[] stringArrToIntArr(String[] s) {
    int[] result = new int[s.length];
    for (int i = 0; i < s.length; i++) {
      result[i] = Integer.parseInt(s[i]);
    }
    return result;
  }

  private static byte[] convertIntToByte(int[] input){
    byte[] result = new byte[input.length];
    for (int i = 0; i < input.length; i++) {
      byte b = (byte) input[i];
      result[i] = b;
    }
    return result;
  }

  private static byte[] convertStringToByte(String data){
    String[] keyInt_S = data.split(",");
    int[] keyInt = new int[36];
    keyInt = stringArrToIntArr(keyInt_S);

    byte[] bytes = convertIntToByte(keyInt);
    return bytes;
  }

  @ReactMethod
  public void init(String identityString, String storePath, Promise promise) {
    ThreadUtils.runOnExecutor(() -> {
      try{
        Log.d("init",storePath);
        WritableArray arr = new WritableNativeArray();
        byte[] identity = toByte(identityString);
        initInternal(identity, storePath);
        promise.resolve(true);
      }
      catch(Exception e){
        promise.reject(e);
        Log.d("init",e.getMessage());
      }
    });
  }

  @Nullable
  private byte[] createPeerIdentity(byte[] privateKey) {
      try {
        // TODO: First: create public key from provided private key
        // TODO: Should read the local keychain store (if it is key-value, key is public key above,
        // TODO: if found, decrypt using the private key
        // TODO: If not found or decryption not successful, generate an identity
        // TODO: then encrypt and store in keychain
        byte[] autoGeneratedIdentity = Fulamobile.generateEd25519Key();
        return autoGeneratedIdentity;
      } catch (Exception e){
        Log.d("initInternal",e.getMessage());
      }
      return null;
  }

  @Nullable
  private Client initInternal(byte[] identity, String storePath) {
      try{
        Config config_ext = new Config();
        if(storePath == null || storePath.trim().isEmpty()) {
          config_ext.setStorePath(fulaStorePath);
        }else{
          config_ext.setStorePath(storePath);
        }

        byte[] peerIdentity = config_ext.getIdentity();
        if (peerIdentity == null || peerIdentity.length == 0) {
          peerIdentity = createPeerIdentity(identity);
          config_ext.setIdentity(peerIdentity);
        }
        this.fula = Fulamobile.newClient(config_ext);
        return this.fula;
      }
      catch(Exception e){
        Log.d("initInternal",e.getMessage());
      }
      return null;
  }

    @ReactMethod
    public void get(String keyString, Promise promise) {
      ThreadUtils.runOnExecutor(() -> {
        Log.d("ReactNative", "get: keystring = "+keyString);
        try{
          byte[] key = convertStringToByte(keyString);
          byte[] value = getInternal(key);
          String valueString = toString(value);
          promise.resolve(valueString);
        }
        catch(Exception e){
          promise.reject(e);
          Log.d("get",e.getMessage());
        }
      });
    }

    @Nullable
    private byte[] getInternal(byte[] key) {
            try{
              Log.d("ReactNative", "getInternal: key.toString() = "+toString(key));
              Log.d("ReactNative", "getInternal: key.toString().bytes = "+ Arrays.toString(key));
              byte[] value = fula.get(key);
              Log.d("ReactNative", "getInternal: value.toString() = "+toString(value));
              return value;
            }
            catch(Exception e){
              Log.d("ReactNative", "getInternal: error = "+e.getMessage());
              Log.d("getInternal",e.getMessage());
            }
      return null;
    }

    private void hasInternal(byte[] key, Promise promise) {
      ThreadUtils.runOnExecutor(() -> {
          try{
            boolean res = fula.has(key);
            promise.resolve(res);
          }
          catch(Exception e){
            promise.reject(e);
            Log.d("hasInternal",e.getMessage());
          }
       });
    }

    public void pullInternal(String addr, byte[] key, Promise promise) {
        ThreadUtils.runOnExecutor(() -> {
          try{
            fula.pull(addr, key);
            promise.resolve(true);
          }
          catch(Exception e){
            promise.reject(e);
            Log.d("pullInternal",e.getMessage());
          }
      });
    }

    public void pushInternal(String addr, byte[] key, Promise promise){
        ThreadUtils.runOnExecutor(() -> {
          try{
            fula.push(addr, key);
            promise.resolve(true);
          }catch (Exception e){
            promise.reject(e);
            Log.d("pushInternal",e.getMessage());
          }
        });
    }

    @ReactMethod
    public void put(String keyString, String valueString, Promise promise) {
      ThreadUtils.runOnExecutor(() -> {
        Log.d("ReactNative", "put: keystring = "+keyString);
        Log.d("ReactNative", "put: valueString = "+valueString);
        try{
          byte[] key = convertStringToByte(keyString);

          Log.d("ReactNative", "put: key.toString() = "+toString(key));
          byte[] value = toByte(valueString);

          Log.d("ReactNative", "put: value.toString() = "+toString(value));
          putInternal(key, value);
          promise.resolve(true);
        }catch (Exception e){
          promise.reject(e);
          Log.d("ReactNative", "put: error = "+e.getMessage());
          Log.d("put",e.getMessage());
        }
      });
    }

    @Nullable
    private byte[] putInternal(byte[] key, byte[] value) {
          try{
            fula.put(key, value);
            return key;
          }catch (Exception e){
            Log.d("putInternal",e.getMessage());
          }
          return null;
    }

    @ReactMethod
    public void shutdown(Promise promise) {
        ThreadUtils.runOnExecutor(() -> {
          try{
            fula.shutdown();
            promise.resolve(true);
          }catch (Exception e){
            promise.reject(e);
            Log.d("shutdown",e.getMessage());
          }
          });

    }

}
