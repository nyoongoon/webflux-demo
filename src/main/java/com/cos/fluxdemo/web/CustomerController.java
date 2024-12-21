package com.cos.fluxdemo.web;

import com.cos.fluxdemo.domain.Customer;
import com.cos.fluxdemo.domain.CustomerRepository;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@RestController
public class CustomerController {
    private final CustomerRepository customerRepository;
    private final Sinks.Many<Customer> sink;

    // Sink 개념
    // A 요청 -> Flux -> Stream
    // B 요청 -> Flux -> Stream
    // 두 스트림을 머지
    // -> Flux.merge  -> sink 맞춰짐 -> Stream을 합쳐줌

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
        //새로 push된 데이터만 구독자에게 전송해주는 방식
        sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    @GetMapping("/flux")
    public Flux<Integer> flux() {
        return Flux.just(1, 2, 3, 4, 5).delayElements(Duration.ofSeconds(1)).log(); //5초가 지난 뒤 한번에 리턴
    }

    //    @GetMapping(value = "/flux-stream", produces = MediaType.APPLICATION_STREAM_JSON_VALUE) //강의에선 이렇게 해도 Flux응답 됐는데 나는 안됌...
    @GetMapping(value = "/flux-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Integer> fluxStream() {
        return Flux.just(1, 2, 3, 4, 5).delayElements(Duration.ofSeconds(1)).log(); //1초에 한 번씩 순차적으로 5번 보냄
    }

    @GetMapping("/customer/{id}") //단건인 경우 Mono
    public Mono<Customer> findById(@PathVariable long id) {
        return customerRepository.findById(id).log();
    }

    //    @GetMapping(value = "/customer-stream", produces = MediaType.APPLICATION_STREAM_JSON_VALUE) //강의에선 이렇게 해도 Flux응답 됐는데 나는 안됌..
    @GetMapping(value = "/customer-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Customer> findAll() {
        return customerRepository.findAll().delayElements(Duration.ofSeconds(1)).log();
    }

    //    @GetMapping(value = "/customer/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @GetMapping(value = "/customer/sse") //ServerSentEvent 타입 리턴 시 MediaType 생략가능
    public Flux<ServerSentEvent<Customer>> findAllSSE() {
        //sink된 곳에 들어온 Flux 데이터가 있는지를 map()으로 확인
        return sink.asFlux().map(c -> ServerSentEvent.builder(c).build())
                //아래 코드가 없으면 해당 url로 재요청이 불가..
                .doOnCancel(
                        //캔슬 시 onComplete() 호출됨
                        () -> sink.asFlux().blockLast() //마지막 데이터라고 알려주는 것
                );
    }

    @PostMapping("customer")
    public Mono<Customer> save() {
        return customerRepository.save(new Customer("gildong", "hong"))
                //sink에 데이터 push하기
                .doOnNext(c -> {
                    sink.tryEmitNext(c); //emit하여 데이터 publish하기
                });
    }
}
