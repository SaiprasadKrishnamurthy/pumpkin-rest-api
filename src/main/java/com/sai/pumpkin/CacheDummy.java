package com.sai.pumpkin;

import redis.clients.jedis.Jedis;

/**
 * Created by saipkri on 08/03/17.
 */
public class CacheDummy {
    public static void mains(String[] args) {
        Jedis jedis = new Jedis("10.126.219.142");

        System.out.println("Connection to server sucessfully");
        //check whether server is running or not
        System.out.println("Server is running: "+jedis.ping());
        jedis.set("tutorial-name", "Redis tutorial");
        System.out.println(jedis.get("tutorial-name"));
    }
}
