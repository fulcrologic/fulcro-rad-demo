CREATE TYPE state AS ENUM (
 'GA', 'DE', 'WA', 'HI', 'AL', 'CA', 'OR', 'KS', 'MO', 'CT', 'AZ', 'AK', 'MS'
);

CREATE TABLE addresses (
  id         UUID         not null primary key,
  street     VARCHAR(255) not null,
  city       VARCHAR(255) not null,
  state      state        not null,
  zip        TEXT         not null,
  account_id UUID         references accounts
);

CREATE INDEX addresses_account_id_idx ON addresses(account_id);
