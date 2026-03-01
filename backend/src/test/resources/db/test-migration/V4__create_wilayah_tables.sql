CREATE TABLE provinces (
  province_id VARCHAR(10) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE states (
  state_id VARCHAR(10) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  province_id VARCHAR(10) NOT NULL REFERENCES provinces(province_id),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_states_province ON states(province_id);

CREATE TABLE districts (
  district_id VARCHAR(10) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  state_id VARCHAR(10) NOT NULL REFERENCES states(state_id),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_districts_state ON districts(state_id);

CREATE TABLE subdistricts (
  subdistrict_id VARCHAR(10) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  district_id VARCHAR(10) NOT NULL REFERENCES districts(district_id),
  zip_code VARCHAR(10),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_subdistricts_district ON subdistricts(district_id);
CREATE INDEX idx_subdistricts_zip ON subdistricts(zip_code);
