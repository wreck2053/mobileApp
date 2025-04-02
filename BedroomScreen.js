import React, {useState, useCallback} from 'react';
import {useFocusEffect} from '@react-navigation/native';
import {View, Text, StyleSheet, TouchableOpacity, Alert} from 'react-native';
import {Card} from 'react-native-paper';
import Slider from '@react-native-community/slider';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import AsyncStorage from '@react-native-async-storage/async-storage';

const MyBedroomScreen = ({navigation}) => {
  const [acPower, setAcPower] = useState(false);
  const [temperature, setTemperature] = useState(24);
  const [fanSpeed, setFanSpeed] = useState('low');

  // Preset values from AsyncStorage
  const [presetTemp, setPresetTemp] = useState(null);
  const [presetFanSpeed, setPresetFanSpeed] = useState('');
  const [presetLed, setPresetLed] = useState('');
  const [presetSwing, setPresetSwing] = useState('');
  const [presetIp, setPresetIp] = useState(null);

  const sendCommand = async command => {
    if (!presetIp) {
      Alert.alert(
        'Error',
        'Device IP is not set. Please configure it in Settings.',
      );
      return false;
    }

    try {
      const response = await fetch(`http://${presetIp}${command}`);

      if (!response.ok) {
        throw new Error(`Server responded with status ${response.status}`);
      }

      console.log('Command sent successfully:', command);
      return true;
    } catch (error) {
      console.error('Failed to send command:', error);
      Alert.alert(
        'Connection Error',
        `Failed to communicate with the device at ${presetIp}. Please check your network and try again.\n\nError: ${error.message}`,
      );
      return false;
    }
  };

  const executePresetAC = async () => {
    if (!presetIp) {
      Alert.alert(
        'Error',
        'Device IP is not set. Please configure it in Settings.',
      );
      return false;
    }

    try {
      console.log('Checking preset values...');

      // Validate Temperature
      if (
        presetTemp === null ||
        isNaN(presetTemp) ||
        presetTemp < 17 ||
        presetTemp > 30
      ) {
        Alert.alert(
          'Error',
          'Invalid temperature. Please select a value between 17°C and 30°C.',
        );
        return;
      }

      // Validate Fan Speed
      if (!['Low', 'Medium', 'High'].includes(presetFanSpeed)) {
        Alert.alert(
          'Error',
          'Invalid fan speed. Please select Low, Medium, or High.',
        );
        return;
      }

      // Validate LED
      if (!['Enabled', 'Disabled'].includes(presetLed)) {
        Alert.alert(
          'Error',
          'Invalid LED setting. Please select Enabled or Disabled.',
        );
        return;
      }

      // Validate Swing
      if (!['Enabled', 'Disabled'].includes(presetSwing)) {
        Alert.alert(
          'Error',
          'Invalid Swing setting. Please select Enabled or Disabled.',
        );
        return;
      }

      console.log('All values are valid. Sending preset commands...');
      setAcPower(true);

      // Temperature Command
      const tempResponse = await sendCommand(`/temp/set/${presetTemp}`);
      if (!tempResponse) return;
      setTemperature(presetTemp); // Update local state

      // Fan Speed Command
      const fanSpeedValue =
        presetFanSpeed === 'Low'
          ? 'low'
          : presetFanSpeed === 'Medium'
          ? 'med'
          : 'high';
      const fanResponse = await sendCommand(`/fan/${fanSpeedValue}`);
      if (!fanResponse) return;
      setFanSpeed(fanSpeedValue); // Update local state

      // LED Command
      if (presetLed === 'Enabled') {
        const ledResponse = await sendCommand('/state/led');
        if (!ledResponse) return;
      }

      // Swing Command
      if (presetSwing === 'Enabled') {
        const swingResponse = await sendCommand('/state/swing');
        if (!swingResponse) return;
      }

      console.log('All preset commands executed successfully.');
    } catch (error) {
      console.error('Failed to execute preset AC commands:', error);
      Alert.alert(
        'Execution Error',
        'An error occurred while executing preset AC commands. Please check your connection and try again.',
      );
    }
  };

  const toggleFanSpeed = () => {
    const nextSpeed =
      fanSpeed === 'low' ? 'med' : fanSpeed === 'med' ? 'high' : 'low';
    setFanSpeed(nextSpeed);
    sendCommand(`/fan/${nextSpeed}`);
  };

  // AsyncStorage function to load stored settings
  const loadSettings = async () => {
    try {
      const storedTemp = await AsyncStorage.getItem('presetTemp');
      const storedFanSpeed = await AsyncStorage.getItem('presetFanSpeed');
      const storedLed = await AsyncStorage.getItem('presetLed');
      const storedSwing = await AsyncStorage.getItem('presetSwing');
      const storedIp = await AsyncStorage.getItem('presetIp');

      console.log('Fetched values:', {
        storedTemp,
        storedFanSpeed,
        storedLed,
        storedSwing,
        storedIp,
      });

      if (storedTemp !== null) setPresetTemp(parseInt(storedTemp));
      if (storedFanSpeed !== null) setPresetFanSpeed(storedFanSpeed);
      if (storedLed !== null) setPresetLed(storedLed);
      if (storedSwing !== null) setPresetSwing(storedSwing);
      if (storedIp !== null) setPresetIp(storedIp);
    } catch (error) {
      console.error('Failed to load settings:', error);
    }
  };

  // Use useFocusEffect to load settings when the screen is focused
  useFocusEffect(
    useCallback(() => {
      loadSettings();
    }, []),
  );

  return (
    <View style={styles.appContainer}>
      {/* App Header */}
      <Text style={styles.appHeader}>Smart Home Control</Text>

      {/* Switches - Light & Fan */}
      <Card style={styles.switchCard}>
        <View style={styles.switchHeader}>
          <Text style={styles.switchTitle}>Switches</Text>
        </View>
        <View style={styles.switchContainer}>
          <TouchableOpacity
            style={styles.switchButtonContainer}
            onPress={() => sendCommand('/toggle-light')}>
            <Icon name="ceiling-light" size={40} color="white" />
            <Text style={styles.switchButtonText}>Light</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.switchButtonContainer}
            onPress={() => sendCommand('/toggle-fan')}>
            <Icon name="ceiling-fan" size={40} color="white" />
            <Text style={styles.switchButtonText}>Fan</Text>
          </TouchableOpacity>
        </View>
      </Card>

      {/* AC Controls */}
      <Card style={styles.acCard}>
        <View style={styles.acHeader}>
          <Text style={styles.acTitle}>Air Conditioner</Text>
          {/* Power Button */}
          <TouchableOpacity
            onPress={() => {
              const newPowerState = !acPower;
              setAcPower(newPowerState);
              sendCommand(newPowerState ? '/power/on' : '/power/off');
            }}>
            <Icon
              name={acPower ? 'power' : 'power'}
              size={32}
              color={acPower ? 'green' : 'red'}
            />
          </TouchableOpacity>
        </View>
        <View style={styles.acControls}>
          <Text style={styles.acTemperatureText}>
            Temperature : {temperature}°C
          </Text>
          {/* Temperature Slider */}
          <Slider
            style={styles.slider}
            value={temperature}
            onValueChange={setTemperature}
            onSlidingComplete={value => {
              setAcPower(true);
              sendCommand(`/temp/set/${value}`);
            }}
            minimumValue={17}
            maximumValue={30}
            step={1}
            minimumTrackTintColor="#00c3ff"
            maximumTrackTintColor="#555"
            thumbTintColor="#00c3ff"
          />
          <View style={styles.acIconContainer}>
            <View style={styles.acIconRow}>
              {/* Cool */}
              <TouchableOpacity
                style={styles.acIconWrapper}
                onPress={() => {
                  setAcPower(true);
                  sendCommand('/mode/cool');
                }}>
                <Icon name="snowflake" size={32} color="white" />
                <Text style={styles.acIconText}>Cool</Text>
              </TouchableOpacity>

              {/* Turbo */}
              <TouchableOpacity
                style={styles.acIconWrapper}
                onPress={() => {
                  sendCommand('/state/turbo');
                }}>
                <Icon name="rocket" size={32} color="white" />
                <Text style={styles.acIconText}>Turbo</Text>
              </TouchableOpacity>

              {/* Preset */}
              <TouchableOpacity
                style={styles.acIconWrapper}
                onPress={executePresetAC}>
                <Icon name="air-conditioner" size={32} color="white" />
                <Text style={styles.acIconText}>Preset</Text>
              </TouchableOpacity>
            </View>
            <View style={styles.acIconRow}>
              {/* LED */}
              <TouchableOpacity
                style={styles.acIconWrapper}
                onPress={() => sendCommand('/state/led')}>
                <Icon name="led-on" size={32} color="white" />
                <Text style={styles.acIconText}>LED</Text>
              </TouchableOpacity>

              {/* Swing */}
              <TouchableOpacity
                style={styles.acIconWrapper}
                onPress={() => sendCommand('/state/swing')}>
                <Icon name="swap-horizontal" size={32} color="white" />
                <Text style={styles.acIconText}>Swing</Text>
              </TouchableOpacity>
            </View>
            <View style={styles.acIconRow}>
              {/* Fan Speed */}
              <TouchableOpacity
                style={styles.acIconWrapper}
                onPress={() => {
                  toggleFanSpeed();
                  setAcPower(true);
                }}>
                <Icon
                  name={
                    fanSpeed === 'low'
                      ? 'fan-speed-1'
                      : fanSpeed === 'med'
                      ? 'fan-speed-2'
                      : 'fan-speed-3'
                  }
                  size={32}
                  color="white"
                />
                <Text style={styles.acIconText}>
                  Fan Speed:{' '}
                  {fanSpeed === 'low'
                    ? 'Low'
                    : fanSpeed === 'med'
                    ? 'Medium'
                    : 'High'}
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Card>

      {/* Night Lamp */}
      <Card style={styles.nightLampCard}>
        <TouchableOpacity
          style={styles.nightLampHeader}
          onPress={() => sendCommand('/toggle-nl')}>
          <Text style={styles.nightLampTitle}>Night Lamp</Text>
          <Icon name="lamp" size={40} color="white" />
        </TouchableOpacity>
      </Card>

      {/* Settings */}
      <Card style={styles.settingsCard}>
        <TouchableOpacity
          style={styles.settingsHeader}
          onPress={() => navigation.navigate('Settings')}>
          <Text style={styles.settingsTitle}>Settings</Text>
          <Icon name="cog" size={40} color="white" />
        </TouchableOpacity>
      </Card>
    </View>
  );
};

const styles = StyleSheet.create({
  appContainer: {
    flex: 1,
    backgroundColor: '#121212',
    padding: 20,
  },
  appHeader: {
    fontSize: 28,
    fontWeight: 'bold',
    color: 'white',
    textAlign: 'center',
    marginBottom: 20,
    padding: 10,
  },
  switchCard: {
    backgroundColor: '#1E1E1E',
    padding: 20,
    borderRadius: 10,
    marginBottom: 15,
  },
  switchHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 15,
  },
  switchTitle: {
    fontSize: 20,
    color: 'white',
    fontWeight: 'bold',
  },
  switchContainer: {
    flexDirection: 'row',
    justifyContent: 'space-evenly',
    width: '100%',
    alignItems: 'center',
  },
  switchButtonContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    flex: 1,
    marginHorizontal: 10,
  },
  switchButtonText: {
    color: 'white',
    marginTop: 10,
    fontSize: 16,
    textAlign: 'center',
  },
  acCard: {
    backgroundColor: '#1E1E1E',
    padding: 20,
    borderRadius: 10,
    marginBottom: 15,
  },
  acHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 15,
  },
  acTitle: {
    fontSize: 20,
    color: 'white',
    fontWeight: 'bold',
  },
  acControls: {
    paddingLeft: 5,
    paddingRight: 5,
  },
  acTemperatureText: {
    color: 'white',
    fontSize: 16,
    marginBottom: 10,
  },
  acIconContainer: {
    marginTop: 20,
    alignItems: 'center',
  },
  acIconRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    width: '100%',
    marginTop: 10,
  },
  acIconWrapper: {
    alignItems: 'center',
  },
  acIconText: {
    color: 'white',
    fontSize: 12,
    marginTop: 5,
  },
  nightLampCard: {
    backgroundColor: '#1E1E1E',
    padding: 20,
    borderRadius: 10,
    marginBottom: 15,
  },
  nightLampHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  nightLampTitle: {
    fontSize: 20,
    color: 'white',
    fontWeight: 'bold',
  },
  settingsCard: {
    backgroundColor: '#1E1E1E',
    padding: 20,
    borderRadius: 10,
    marginBottom: 15,
  },
  settingsHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  settingsTitle: {
    fontSize: 20,
    color: 'white',
    fontWeight: 'bold',
  },
});

export default MyBedroomScreen;
