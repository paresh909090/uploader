package com.uploader.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.uploader.dto.ResourceDTO;
import com.uploader.service.ResourceService;

@Controller
@RequestMapping("/resource")
public class ResourceController {

	@Value(value = "${resources_base_location}")
	private String resourceBaseLocation;
	
	@Autowired
	private ResourceService resourceService; 
	
	@RequestMapping(value="/add", method=RequestMethod.POST)
	public @ResponseBody ResourceDTO addResource(@ModelAttribute ResourceDTO resourceFromRequest, HttpServletRequest request) throws Exception {
		
		ResourceDTO response = this.resourceService.addResource(resourceFromRequest, request);
		return response;
	}
	
	@RequestMapping(value="/import", method=RequestMethod.POST)
	public @ResponseBody ResourceDTO importZipFile(@ModelAttribute ResourceDTO resourceFromRequest, HttpServletRequest request) throws Exception {
		
		ResourceDTO response = this.resourceService.importZipFile(resourceFromRequest, request);
		return response;
	}
	
	@RequestMapping(value="", method=RequestMethod.GET)
	public @ResponseBody List<ResourceDTO> getAllResources(@RequestParam(name = "p", required = false) Long parentResourceId, HttpServletRequest request) throws Exception {
		
		List<ResourceDTO> response = this.resourceService.getAllResources(parentResourceId, request);
		
		return response;
	}
	
	@RequestMapping(value="/breadcrumb", method=RequestMethod.GET)
	public @ResponseBody List<ResourceDTO> getBreadcrumb(@RequestParam(name = "p", required = false) Long parentResourceId, HttpServletRequest request) throws Exception {
		
		List<ResourceDTO> response = this.resourceService.getBreadcrumb(parentResourceId);
		
		return response;
	}
	
	@GetMapping(value = "/view/{file}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String viewDocuments(@PathVariable("file") String file) {

		return this.resourceService.viewResourceHTML(Long.parseLong(file));
    }
	
	@RequestMapping(value="/download", method=RequestMethod.POST)
	public ResponseEntity<Resource> download(@ModelAttribute ResourceDTO resourceFromRequest, HttpServletResponse response, HttpServletRequest request) throws Exception {
		
		return this.resourceService.download(resourceFromRequest, response, request);
	}
	
	@GetMapping("/delete")
	@ResponseStatus(value = HttpStatus.OK)
	public void deleteDocuments(@RequestParam(name = "p", required = true) List<Long> resourceId) {
		
		this.resourceService.deleteResource(resourceId);
   }
}
