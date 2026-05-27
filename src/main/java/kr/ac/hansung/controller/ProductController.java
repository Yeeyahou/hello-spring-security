package kr.ac.hansung.controller;

import jakarta.validation.Valid;
import kr.ac.hansung.dto.ProductDto;
import kr.ac.hansung.entity.Product;
import kr.ac.hansung.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model) {
        int requestedPage = Math.max(page, 0);
        int pageSize = size > 0 ? size : 5;
        PageRequest pageRequest = PageRequest.of(requestedPage, pageSize, Sort.by("id").ascending());
        String normalizedKeyword = (keyword != null && !keyword.isBlank()) ? keyword : null;

        Page<Product> productPage;
        if (normalizedKeyword != null) {
            productPage = productService.searchProducts(normalizedKeyword, pageRequest);
        } else {
            productPage = productService.getProducts(pageRequest);
        }

        int totalPages = productPage.getTotalPages();
        if (totalPages > 0 && requestedPage >= totalPages) {
            if (normalizedKeyword != null) {
                return "redirect:/products?page=" + (totalPages - 1)
                    + "&size=" + pageSize
                    + "&keyword=" + normalizedKeyword;
            }
            return "redirect:/products?page=" + (totalPages - 1) + "&size=" + pageSize;
        }

        int currentPage = productPage.getNumber();
        List<Integer> pageNumbers = IntStream.range(0, totalPages)
            .boxed()
            .toList();

        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("totalElements", productPage.getTotalElements());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", productPage.getSize());
        model.addAttribute("pageNumbers", pageNumbers);
        model.addAttribute("hasPrevious", productPage.hasPrevious());
        model.addAttribute("hasNext", productPage.hasNext());
        model.addAttribute("previousPage", currentPage - 1);
        model.addAttribute("nextPage", currentPage + 1);
        model.addAttribute("keyword", normalizedKeyword);
        return "products/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.findById(id));
        return "products/detail";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("product", new ProductDto());
        return "products/add";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("product") ProductDto dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "products/add";
        }
        productService.save(dto);
        return "redirect:/products";
    }

    @GetMapping("/{id}/edit")
    public String editProductForm(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);

        ProductDto dto = new ProductDto();
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setDescription(product.getDescription());

        model.addAttribute("productDto", dto);
        model.addAttribute("productId", id);
        return "products/edit";
    }

    @PostMapping("/{id}/edit")
    public String editProduct(@PathVariable Long id,
            @Valid @ModelAttribute ProductDto productDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", id);
            return "products/edit";
        }

        productService.updateProduct(id, productDto);
        ra.addFlashAttribute("successMessage", "상품이 수정되었습니다.");
        return "redirect:/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        productService.deleteById(id);
        return "redirect:/products";
    }
}
