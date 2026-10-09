package com.rollingstone.kc.fbl;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Plain-JDBC data access for the idp_user / idp_user_role / idp_user_group /
 * idp_sync_audit tables. Opens one short-lived connection per call —
 * acceptable for this lab's login volume; swap for a pooled DataSource
 * (HikariCP) in a real deployment.
 */
public class EntraUserSyncDao {

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(
                EntraFblConstants.JDBC_URL,
                EntraFblConstants.JDBC_USER,
                EntraFblConstants.JDBC_PASSWORD);
    }

    /** Insert-or-update idp_user by entra_object_id. Returns the local idp_user.id. */
    public long upsertUser(String keycloakUserId, String entraObjectId, String email,
                           String firstName, String lastName, LocalDate dob, String phone,
                           String street, String city, String state, String postalCode, String country)
            throws SQLException {

        try (Connection c = connect()) {
            Long existingId = findIdByEntraObjectId(c, entraObjectId);

            if (existingId == null) {
                String sql = "INSERT INTO idp_user (keycloak_user_id, entra_object_id, email, first_name, " +
                        "last_name, date_of_birth, phone_number, street_address, city, state, postal_code, country) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, keycloakUserId);
                    ps.setString(2, entraObjectId);
                    ps.setString(3, email);
                    ps.setString(4, firstName);
                    ps.setString(5, lastName);
                    setNullableDate(ps, 6, dob);
                    ps.setString(7, phone);
                    ps.setString(8, street);
                    ps.setString(9, city);
                    ps.setString(10, state);
                    ps.setString(11, postalCode);
                    ps.setString(12, country);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        long newId = keys.getLong(1);
                        recordAudit(c, newId, "USER_CREATED", email);
                        return newId;
                    }
                }
            } else {
                String sql = "UPDATE idp_user SET keycloak_user_id=?, email=?, first_name=?, last_name=?, " +
                        "date_of_birth=?, phone_number=?, street_address=?, city=?, state=?, postal_code=?, " +
                        "country=?, last_login_at=CURRENT_TIMESTAMP WHERE id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, keycloakUserId);
                    ps.setString(2, email);
                    ps.setString(3, firstName);
                    ps.setString(4, lastName);
                    setNullableDate(ps, 5, dob);
                    ps.setString(6, phone);
                    ps.setString(7, street);
                    ps.setString(8, city);
                    ps.setString(9, state);
                    ps.setString(10, postalCode);
                    ps.setString(11, country);
                    ps.setLong(12, existingId);
                    ps.executeUpdate();
                }
                recordAudit(c, existingId, "USER_UPDATED", email);
                return existingId;
            }
        }
    }

    /** Records a fresh login timestamp without touching profile fields. */
    public void touchLastLogin(long idpUserId) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE idp_user SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setLong(1, idpUserId);
            ps.executeUpdate();
        }
    }

    public Long findIdByEntraObjectId(String entraObjectId) throws SQLException {
        try (Connection c = connect()) {
            return findIdByEntraObjectId(c, entraObjectId);
        }
    }

    private Long findIdByEntraObjectId(Connection c, String entraObjectId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM idp_user WHERE entra_object_id = ?")) {
            ps.setString(1, entraObjectId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    public Set<String> currentActiveRoles(long idpUserId) throws SQLException {
        return currentActive(idpUserId, "idp_user_role", "role_name");
    }

    public Set<String> currentActiveGroups(long idpUserId) throws SQLException {
        return currentActive(idpUserId, "idp_user_group", "group_name");
    }

    private Set<String> currentActive(long idpUserId, String table, String column) throws SQLException {
        Set<String> values = new HashSet<>();
        String sql = "SELECT " + column + " FROM " + table + " WHERE idp_user_id = ? AND revoked_at IS NULL";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, idpUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    values.add(rs.getString(1));
                }
            }
        }
        return values;
    }

    public void grantRole(long idpUserId, String roleName) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO idp_user_role (idp_user_id, role_name) VALUES (?, ?)")) {
            ps.setLong(1, idpUserId);
            ps.setString(2, roleName);
            ps.executeUpdate();
            recordAudit(c, idpUserId, "ROLE_GRANTED", roleName);
        }
    }

    public void revokeRole(long idpUserId, String roleName) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE idp_user_role SET revoked_at = CURRENT_TIMESTAMP " +
                             "WHERE idp_user_id = ? AND role_name = ? AND revoked_at IS NULL")) {
            ps.setLong(1, idpUserId);
            ps.setString(2, roleName);
            ps.executeUpdate();
            recordAudit(c, idpUserId, "ROLE_REVOKED", roleName);
        }
    }

    public void joinGroup(long idpUserId, String groupName) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO idp_user_group (idp_user_id, group_name) VALUES (?, ?)")) {
            ps.setLong(1, idpUserId);
            ps.setString(2, groupName);
            ps.executeUpdate();
            recordAudit(c, idpUserId, "GROUP_JOINED", groupName);
        }
    }

    public void leaveGroup(long idpUserId, String groupName) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE idp_user_group SET revoked_at = CURRENT_TIMESTAMP " +
                             "WHERE idp_user_id = ? AND group_name = ? AND revoked_at IS NULL")) {
            ps.setLong(1, idpUserId);
            ps.setString(2, groupName);
            ps.executeUpdate();
            recordAudit(c, idpUserId, "GROUP_LEFT", groupName);
        }
    }

    private void recordAudit(Connection c, long idpUserId, String eventType, String detail) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO idp_sync_audit (idp_user_id, event_type, detail) VALUES (?, ?, ?)")) {
            ps.setLong(1, idpUserId);
            ps.setString(2, eventType);
            ps.setString(3, detail);
            ps.executeUpdate();
        }
    }

    private void setNullableDate(PreparedStatement ps, int index, LocalDate date) throws SQLException {
        if (date == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(date));
        }
    }
}