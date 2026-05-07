package com.kisquant.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
final class PageController {

	@GetMapping("/kis")
	String kis() {
		return "forward:/kis/index.html";
	}
}
