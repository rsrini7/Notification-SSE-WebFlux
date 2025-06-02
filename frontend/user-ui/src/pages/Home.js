import React from 'react';
import CountrySelector from '../components/CountrySelector';

const Home = () => {
  return (
    <div>
      <h1>Welcome to the Home Page</h1>
      <p>Select your country:</p>
      <CountrySelector />
    </div>
  );
};

export default Home;
