Update 2:

1.Activate User:

powershell -ExecutionPolicy Bypass -File .\run-db-query.ps1 "UPDATE users SET status = 'ACTIVE' WHERE email = 'amrutha@atithi.com';"

2.Activate Hostel:

powershell -ExecutionPolicy Bypass -File .\run-db-query.ps1 "UPDATE hostels SET status = 'ACTIVE' WHERE id = (SELECT hostel_id FROM users WHERE email = 'amrutha@atithi.com');"
