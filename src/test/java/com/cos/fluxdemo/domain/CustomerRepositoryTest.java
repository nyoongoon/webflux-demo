package com.cos.fluxdemo.domain;

import com.cos.fluxdemo.DbInit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Import(DbInit.class)
class CustomerRepositoryTest {
    @Autowired
    private CustomerRepository customerRepository;

    @Test
    public void 한건찾기_테스트(){
        StepVerifier
                .create(customerRepository.findById(2L))
                .expectNextMatches((c) -> c.getFirstName().equals("Chloe"))
                .expectComplete()
                .verify();

    }
}