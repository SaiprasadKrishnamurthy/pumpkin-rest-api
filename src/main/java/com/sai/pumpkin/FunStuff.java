package com.sai.pumpkin;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * Created by saipkri on 21/03/17.
 */
public class FunStuff {

    public static void mains(String[] args) throws Exception {
        SimpleDateFormat fmt = new SimpleDateFormat("EEE MMM dd hh:mm:ss ZZ yyyy");

        Files.lines(Paths.get("s.txt"))
                .map(line -> {
                    if (line.contains("#") && line.contains("version")) {
                        line = line.trim();
                        String clipped = line.substring(line.indexOf("#", 2) + 1, line.indexOf("version")).trim();
                        try {
                            System.out.println(line);
                            Date date = fmt.parse(clipped);
                            System.out.println(clipped);
                            System.out.println(date);
                            System.out.println(" ---------------------------- ");
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                        return Optional.of(clipped);
                    } else {
                        return Optional.empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .count();

    }
}
