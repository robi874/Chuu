package core.imagerenderer.util;

import core.parsers.Parser;
import core.parsers.params.CommandParameters;
import dao.entities.ReturnNowPlaying;
import org.knowm.xchart.PieChart;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PieableListKnows<T extends CommandParameters> extends OptionalPie implements IPieableList<ReturnNowPlaying, T> {

    public PieableListKnows(Parser<?> parser) {
        super(parser);
    }


    @Override
    public PieChart fillPie(PieChart chart, T params, List<ReturnNowPlaying> data) {
        int total = data.stream().mapToInt(ReturnNowPlaying::getPlayNumber).sum();
        int breakpoint = (int) (0.75 * total);
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger acceptedCount = new AtomicInteger(0);
        fillListedSeries(chart,
                ReturnNowPlaying::getDiscordName,
                ReturnNowPlaying::getPlayNumber,
                x -> {
                    if (acceptedCount.get() < 10 || (counter.get() < breakpoint && acceptedCount.get() < 15)) {
                        counter.addAndGet(x.getPlayNumber());
                        acceptedCount.incrementAndGet();
                        return true;
                    } else {
                        return false;
                    }
                }, data);
        return chart;
    }


}
