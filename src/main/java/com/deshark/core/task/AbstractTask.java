package com.deshark.core.task;

import com.deshark.core.schemas.ModpackFile;

import java.util.concurrent.Callable;

public abstract class AbstractTask implements Callable<ModpackFile> {

    public AbstractTask() {
    }

    @Override
    public abstract ModpackFile call() throws Exception;
}
