INSERT INTO wallets (id,owner_id,currency,balance,status) VALUES
                                                              ('a0000000-0000-0000-0000-000000000001','user_alice','INR',10000.0000,'ACTIVE'),
                                                              ('a0000000-0000-0000-0000-000000000002','user_bob',  'INR', 5000.0000,'ACTIVE'),
                                                              ('a0000000-0000-0000-0000-000000000003','user_carol','INR', 2000.0000,'ACTIVE'),
                                                              ('a0000000-0000-0000-0000-000000000004','user_dave', 'INR',    0.0000,'ACTIVE'),
                                                              ('a0000000-0000-0000-0000-000000000005','user_eve',  'INR', 1000.0000,'SUSPENDED')
    ON CONFLICT (id) DO NOTHING;