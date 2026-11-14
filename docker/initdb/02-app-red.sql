-- La imagen oficial crea app_rula@'%' con MYSQL_USER/MYSQL_PASSWORD antes del SQL.
-- El script original crea app_rula@localhost; este ajuste limita la cuenta de red.
-- No contiene claves y no cambia tablas ni datos del esquema aprobado.
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'app_rula'@'%';
GRANT SELECT, INSERT, UPDATE ON rula_ergonomia.* TO 'app_rula'@'%';
