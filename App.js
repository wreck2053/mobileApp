import * as React from 'react';
import {NavigationContainer} from '@react-navigation/native';
import {createStackNavigator, TransitionPresets} from '@react-navigation/stack';
import BedroomScreen from './BedroomScreen';
import SettingsScreen from './SettingsScreen';

const Stack = createStackNavigator();

const App = () => {
  return (
    <NavigationContainer>
      <Stack.Navigator
        initialRouteName="Bedroom"
        screenOptions={{
          headerShown: false,
          headerStyle: {backgroundColor: '#434343'},
          headerTintColor: '#f4f4f9',
          headerTitleStyle: {fontWeight: 'bold'},
          ...TransitionPresets.SlideFromRightIOS,
        }}>
        <Stack.Screen
          name="Bedroom"
          component={BedroomScreen}
          options={{title: 'Bedroom'}}
        />
        <Stack.Screen
          name="Settings"
          component={SettingsScreen}
          options={{title: 'Settings'}}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
};

export default App;
