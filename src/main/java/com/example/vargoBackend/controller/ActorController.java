package com.example.vargoBackend.controller;

import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.vargoBackend.model.Movie;
import com.example.vargoBackend.repo.MovieRepository;

@RestController
public class ActorController {

    private final MovieRepository repo;

        public ActorController(MovieRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/test")
    public int getOne() {
        return 1;
    }

    @GetMapping("/actor/{name}/movies")
    public List<Movie> moviesByActor(@PathVariable String name) {
        return repo.findMoviesByActorName(name);
    }
}