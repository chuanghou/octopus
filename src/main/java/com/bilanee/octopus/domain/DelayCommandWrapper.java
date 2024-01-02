package com.bilanee.octopus.domain;

import com.stellariver.milky.domain.support.command.Command;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DelayCommandWrapper implements Delayed {

    private Command command;
    private Date executeDate;

    public DelayCommandWrapper(Command command, Date executeDate) {
        this.command = command;
        this.executeDate = executeDate;
    }

    @Override
    public long getDelay(TimeUnit timeUnit) {
        return timeUnit.convert(executeDate.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NonNull Delayed o) {
        return Long.compare(executeDate.getTime(), ((DelayCommandWrapper) o).getExecuteDate().getTime());
    }

}
