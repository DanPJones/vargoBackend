package com.example.vargoBackend.repo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.example.vargoBackend.model.User;

import org.springframework.jdbc.core.RowMapper;

@Repository
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public class InsufficientFundsException extends RuntimeException {

        public InsufficientFundsException() {
            super("Insufficient balance");
        }
    }

        public class CreditException extends RuntimeException {

        public CreditException() {
            super("Could not credit user");
        }
    }

    private final RowMapper<User> userMapper = (rs, i) -> new User(
            rs.getLong("id"),
            rs.getString("steam_id"),
            rs.getString("username"),
            rs.getBigDecimal("balance"),
            rs.getTimestamp("created_at").toInstant()
    );

    public Optional<User> findById(Long id) {
        System.out.println("findById() id=" + id);
        String sql = """
            SELECT id, steam_id, username, balance, created_at
            FROM users
            WHERE id = :id
        """;

        var rows = jdbc.query(sql, Map.of("id", id), userMapper);
        return rows.stream().findFirst();
    }

    public Optional<User> findBySteamId(String steamId) {
        System.out.println("findBySteamId() steamId=" + steamId);
        String sql = """
            SELECT id, steam_id, username, balance, created_at
            FROM users
            WHERE steam_id = :steamId
        """;

        var rows = jdbc.query(sql, Map.of("steamId", steamId), userMapper);
        return rows.stream().findFirst();
    }

    public User saveNewUser(String steamId, String username) {
        String sql = """
            INSERT INTO users (steam_id, username, balance)
            VALUES (:steamId, :username, :balance)
        """;

        BigDecimal balance = BigDecimal.valueOf(1000);

        var params = new MapSqlParameterSource()
                .addValue("steamId", steamId)
                .addValue("username", username)
                .addValue("balance", balance);

        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder);

        Long id = keyHolder.getKey().longValue();
        Instant nowUtc = Instant.now();

        return new User(id, steamId, username, balance, nowUtc);
    }

    public BigInteger getUserBalance(Long steamId) {
        String sql = """
            SELECT balance
            FROM users
            WHERE steam_id = :steamId
            LIMIT 1
        """;

        return jdbc.queryForObject(sql,
                Map.of("steamId", steamId),
                BigInteger.class);

        // return bal;
    }

    public BigInteger debitBalance(Long steamId, BigInteger wager) {
        String update = """
        UPDATE users
        SET    balance = balance - :amount
        WHERE  steam_id = :steamId
          AND  balance  >= :amount
    """;

        Map<String, Object> p = Map.of("amount", wager,
                "steamId", steamId);

        int rows = jdbc.update(update, p);
        if (rows == 0) {
            throw new InsufficientFundsException();
        }

        String select = "SELECT balance FROM users WHERE steam_id = :steamId";
        return jdbc.queryForObject(select, p, BigInteger.class);
    }

    public BigInteger creditBalance(Long steamId, BigInteger credit, int multiplier) {
        String update = """
        UPDATE users
        SET balance = balance + (:credit * :multiplier)
        WHERE steam_id = :steamId
         """;

        Map<String, Object> p = Map.of("credit", credit, "steamId", steamId, "multiplier", multiplier);
        int rows = jdbc.update(update, p);
        if (rows == 0) {
            throw new CreditException();
        }
        String select = "SELECT balance FROM users WHERE steam_id = :steamId";
        return jdbc.queryForObject(select, p, BigInteger.class);
    }

}
