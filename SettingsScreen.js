import React, {useState, useCallback} from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Animated,
  Modal,
  TextInput,
} from 'react-native';
import {useFocusEffect} from '@react-navigation/native';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import AsyncStorage from '@react-native-async-storage/async-storage';

const SettingsScreen = ({navigation}) => {
  // Preset values to AsyncStorage
  const [presetTemp, setPresetTemp] = useState(null);
  const [presetFanSpeed, setPresetFanSpeed] = useState('');
  const [presetLed, setPresetLed] = useState('');
  const [presetSwing, setPresetSwing] = useState('');
  const [presetIp, setPresetIp] = useState(null);

  // Screen states
  const [isDropdownVisible, setDropdownVisible] = useState(false);
  const [dropdownHeight, setDropdownHeight] = useState(new Animated.Value(0));
  const [tempModalVisible, setTempModalVisible] = useState(false);
  const [tempInput, setTempInput] = useState('');
  const [ipModalVisible, setIpModalVisible] = useState(false);
  const [ipInput, setIpInput] = useState('192.168.0.102');

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

  // AsyncStorage function to store settings
  const saveSetting = async (key, value) => {
    try {
      await AsyncStorage.setItem(key, value.toString());
    } catch (error) {
      console.error(`Failed to save ${key}:`, error);
    }
  };

  // Use useFocusEffect to load settings when the screen is focused
  useFocusEffect(
    useCallback(() => {
      loadSettings();
    }, []),
  );

  const handleIpSubmit = () => {
    const ipRegex =
      /^(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)$/;

    if (ipRegex.test(ipInput)) {
      saveSetting('presetIp', ipInput);
      setPresetIp(ipInput);
      setIpModalVisible(false);
      console.log(`Device IP set to ${ipInput}`);
    } else {
      alert('Please enter a valid IP address (e.g., 192.168.0.102)');
    }
  };

  const toggleDropdown = () => {
    Animated.timing(dropdownHeight, {
      toValue: isDropdownVisible ? 0 : 220,
      duration: 300,
      useNativeDriver: false,
    }).start();
    setDropdownVisible(!isDropdownVisible);
  };

  const handleTemperatureClick = () => {
    setTempInput(presetTemp ? presetTemp.toString() : '');
    setTempModalVisible(true);
  };

  const handleTemperatureSubmit = () => {
    const temp = parseInt(tempInput);
    if (temp >= 17 && temp <= 30) {
      saveSetting('presetTemp', temp);
      setPresetTemp(temp);
      setTempModalVisible(false);
      setTempInput('');
      console.log(`Temperature set to ${temp}°C`);
    } else {
      alert('Please enter a value between 17 and 30');
    }
  };

  const handleFanSpeedClick = () => {
    const nextSpeed =
      presetFanSpeed === 'Low'
        ? 'Medium'
        : presetFanSpeed === 'Medium'
        ? 'High'
        : 'Low';
    saveSetting('presetFanSpeed', nextSpeed);
    setPresetFanSpeed(nextSpeed);
    console.log(`Fan Speed set to ${nextSpeed}`);
  };

  const handleLedClick = () => {
    const nextLed = presetLed === 'Enabled' ? 'Disabled' : 'Enabled';
    saveSetting('presetLed', nextLed);
    setPresetLed(nextLed);
    console.log(`LED set to ${nextLed}`);
  };

  const handleSwingClick = () => {
    const nextSwing = presetSwing === 'Enabled' ? 'Disabled' : 'Enabled';
    saveSetting('presetSwing', nextSwing);
    setPresetSwing(nextSwing);
    console.log(`Swing set to ${nextSwing}`);
  };

  return (
    <View style={styles.appContainer}>
      {/* Settings Header */}
      <View style={styles.appHeader}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Icon name="arrow-left" size={30} color="white" />
        </TouchableOpacity>
        <Text style={styles.appHeaderTitle}>Settings</Text>
      </View>

      {/* Configure Preset Button */}
      <TouchableOpacity style={styles.acPresetButton} onPress={toggleDropdown}>
        <Text style={styles.acPresetText}>Configure Preset for AC</Text>
        <Icon
          name={isDropdownVisible ? 'chevron-up' : 'chevron-down'}
          size={30}
          color="white"
        />
      </TouchableOpacity>

      {/* Preset Dropdown Section */}
      <Animated.View
        style={[
          styles.acPresetDropdown,
          {height: dropdownHeight, marginTop: isDropdownVisible ? 10 : 0},
        ]}>
        {isDropdownVisible && (
          <>
            <TouchableOpacity
              style={styles.acPresetDropdownItem}
              onPress={handleTemperatureClick}>
              <Icon name="thermometer" size={24} color="white" />
              <Text style={styles.acPresetDropdownText}>AC Temperature</Text>
              <Text style={styles.acPresetValueText}>
                {presetTemp ? `${presetTemp}°C` : 'Not Set'}
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.acPresetDropdownItem}
              onPress={handleFanSpeedClick}>
              <Icon name="fan" size={24} color="white" />
              <Text style={styles.acPresetDropdownText}>Fan Speed</Text>
              <Text style={styles.acPresetValueText}>
                {presetFanSpeed ? presetFanSpeed : 'Not Set'}
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.acPresetDropdownItem}
              onPress={handleLedClick}>
              <Icon name="led-on" size={24} color="white" />
              <Text style={styles.acPresetDropdownText}>Turn off LED</Text>
              <Text style={styles.acPresetValueText}>
                {presetLed ? presetLed : 'Not Set'}
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.acPresetDropdownItem}
              onPress={handleSwingClick}>
              <Icon name="swap-horizontal" size={24} color="white" />
              <Text style={styles.acPresetDropdownText}>Swing</Text>
              <Text style={styles.acPresetValueText}>
                {presetSwing ? presetSwing : 'Not Set'}
              </Text>
            </TouchableOpacity>
          </>
        )}
      </Animated.View>

      {/* Temperature Input Modal */}
      <Modal
        animationType="fade"
        transparent={true}
        visible={tempModalVisible}
        onRequestClose={() => setTempModalVisible(false)}>
        <View style={styles.modalOverlay}>
          <View style={styles.modalContainer}>
            <Text style={styles.modalTitle}>
              Please Enter a Temperature (17 - 30°C)
            </Text>
            <TextInput
              style={styles.modalInput}
              keyboardType="numeric"
              placeholder="Enter temperature"
              value={tempInput}
              onChangeText={setTempInput}
            />
            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={styles.modalButton}
                onPress={() => setTempModalVisible(false)}>
                <Text style={styles.modalButtonText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.modalButton}
                onPress={handleTemperatureSubmit}>
                <Text style={styles.modalButtonText}>OK</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* IP Address Input Button */}
      <TouchableOpacity
        style={styles.ipAddressButton}
        onPress={() => {
          setIpInput(presetIp || '');
          setIpModalVisible(true);
        }}>
        <Text style={styles.ipAddressText}>Set Device IP Address</Text>
        <Icon name="access-point-network" size={30} color="white" />
      </TouchableOpacity>

      {/* IP Address Input Modal */}
      <Modal
        animationType="fade"
        transparent={true}
        visible={ipModalVisible}
        onRequestClose={() => setIpModalVisible(false)}>
        <View style={styles.modalOverlay}>
          <View style={styles.modalContainer}>
            <Text style={styles.modalTitle}>Enter Device IP Address</Text>
            <TextInput
              style={styles.modalInput}
              keyboardType="numeric"
              placeholder="e.g., 192.168.0.102"
              value={ipInput}
              onChangeText={setIpInput}
            />
            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={styles.modalButton}
                onPress={() => setIpModalVisible(false)}>
                <Text style={styles.modalButtonText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.modalButton}
                onPress={handleIpSubmit}>
                <Text style={styles.modalButtonText}>OK</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
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
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 20,
  },
  appHeaderTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: 'white',
    marginLeft: 20,
  },
  acPresetButton: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#1E1E1E',
    padding: 15,
    borderRadius: 10,
  },
  acPresetText: {
    color: 'white',
    fontSize: 18,
    fontWeight: 'bold',
  },
  acPresetDropdown: {
    overflow: 'hidden',
    backgroundColor: '#1E1E1E',
    borderRadius: 10,
  },
  acPresetDropdownItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 15,
    borderBottomWidth: 1,
    borderBottomColor: '#333',
  },
  acPresetDropdownText: {
    color: 'white',
    fontSize: 16,
    marginLeft: 10,
    flex: 1,
  },
  acPresetValueText: {
    color: 'white',
    fontSize: 16,
    marginLeft: 10,
    textAlign: 'right',
    flex: 1,
  },
  ipAddressButton: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#1E1E1E',
    padding: 15,
    borderRadius: 10,
    marginTop: 10,
  },
  ipAddressText: {
    color: 'white',
    fontSize: 18,
    fontWeight: 'bold',
  },
  // Modal styles
  modalOverlay: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
  },
  modalContainer: {
    backgroundColor: '#1E1E1E',
    padding: 30,
    borderRadius: 15,
    width: '80%',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.1,
    shadowRadius: 5,
    elevation: 10,
  },
  modalTitle: {
    color: 'white',
    fontSize: 20,
    marginBottom: 20,
    fontWeight: 'bold',
    textAlign: 'center',
  },
  modalInput: {
    width: '100%',
    height: 50,
    borderColor: '#ccc',
    borderWidth: 1,
    backgroundColor: '#333',
    color: 'white',
    marginBottom: 20,
    paddingLeft: 15,
    borderRadius: 10,
    fontSize: 18,
  },
  modalButtons: {
    width: '100%',
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  modalButton: {
    backgroundColor: '#3A3A3A',
    borderRadius: 10,
    paddingVertical: 10,
    paddingHorizontal: 20,
    marginTop: 10,
    width: '45%',
    alignItems: 'center',
  },
  modalButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default SettingsScreen;
