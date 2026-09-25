package com.songs.jobs;

/** How a job is stored in the config file; the job name is the enclosing map key. */
public record JobSpec(String source, String target) {}
