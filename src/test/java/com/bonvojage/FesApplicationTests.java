package com.bonvojage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.web.WebAppConfiguration;

import com.ogb.fes.FesApplication;

import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FesApplication.class)
@WebAppConfiguration
public class FesApplicationTests {

	@Test
	public void contextLoads() {
	}

}
