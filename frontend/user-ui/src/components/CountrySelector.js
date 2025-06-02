import React, { useState, useEffect } from 'react';

const CountrySelector = () => {
  const [countries, setCountries] = useState([]);
  const [selectedCountry, setSelectedCountry] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch('https://restcountries.com/v3.1/all?fields=name,region,flag')
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.json();
      })
      .then(data => {
        // Sort countries by common name
        const sortedData = data.sort((a, b) =>
          a.name.common.localeCompare(b.name.common)
        );
        setCountries(sortedData);
        setIsLoading(false);
      })
      .catch(e => {
        console.error("Error fetching countries:", e);
        setError("Failed to load countries. Please try again later.");
        setIsLoading(false);
      });
  }, []);

  const handleCountryChange = (event) => {
    const countryName = event.target.value;
    if (countryName) {
      const country = countries.find(c => c.name.common === countryName);
      setSelectedCountry(country);
    } else {
      setSelectedCountry(null);
    }
  };

  return (
    <div>
      <select onChange={handleCountryChange} value={selectedCountry ? selectedCountry.name.common : ""}>
        <option value="">Select a Country</option>
        {isLoading ? (
          <option disabled>Loading...</option>
        ) : error ? (
          <option disabled>{error}</option>
        ) : (
          countries.map(country => (
            <option key={country.name.common} value={country.name.common}>
              {country.name.common}
            </option>
          ))
        )}
      </select>
      {selectedCountry && (
        <div style={{ marginTop: '10px', display: 'flex', alignItems: 'center' }}>
          <span style={{ marginRight: '10px' }}>{selectedCountry.flag}</span>
          <span>{selectedCountry.name.common}</span>
        </div>
      )}
      {!selectedCountry && error && (
         <div style={{ marginTop: '10px', color: 'red' }}>
           {error}
         </div>
      )}
    </div>
  );
};

export default CountrySelector;
