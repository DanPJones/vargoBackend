package com.example.vargoBackend.repo;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import com.example.vargoBackend.model.Movie;

@Repository
public class MovieRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public MovieRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Movie> findMoviesByActorName(String actorName) {
        var sql = """
            SELECT m.id, m.title, m.release_year
            FROM movies         AS m
            JOIN actors_movies  AS am ON am.movie_id = m.id
            JOIN actors         AS a  ON a.id       = am.actor_id
            WHERE a.name = :name
        """;

        return jdbc.query(
            sql,
            Map.of("name", actorName),
            (rs, i) -> new Movie(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getInt("release_year"))
        );
    }
}