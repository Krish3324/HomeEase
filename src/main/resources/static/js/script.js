function toggleMenu() {
  document.getElementById("mobileMenu").classList.toggle("show");
}

function scrollTopPage() {
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function scrollBottomPage() {
  window.scrollTo({ top: document.body.scrollHeight, behavior: "smooth" });
}

window.addEventListener("scroll", () => {
  const navbar = document.querySelector(".navbar");
  navbar.classList.toggle("scrolled", window.scrollY > 20);
});

// CLOSE MOBILE MENU AFTER CLICK
const mobileMenu = document.getElementById("mobileMenu");

if (mobileMenu) {
  document.querySelectorAll("#mobileMenu a").forEach(link => {
    link.addEventListener("click", () => {
      mobileMenu.classList.remove("show");
    });
  });
}




/* =========================
   ROLE POPUP HANDLING
========================= */
function openRolePopup() {
  const popup = document.getElementById("rolePopup");
  if (popup) {
    popup.classList.add("show");
  }
}

function closeRolePopup() {
  const popup = document.getElementById("rolePopup");
  if (popup) {
    popup.classList.remove("show");
  }
}

// Close popup when clicking outside the role-box
document.addEventListener("click", (event) => {
  const popup = document.getElementById("rolePopup");
  if (popup && event.target === popup) {
    closeRolePopup();
  }
});

function goToUserRegister() {
  window.location.href = "/register-user";
}

function goToWorkerRegister() {
  window.location.href = "/register-worker";
}


//admin dashboard
function toggleAdminMenu() {
  document.getElementById("adminNavLinks").classList.toggle("active");
}

//navbar for dashboards

function toggleUserMenu() {
    document.getElementById("userNavLinks").classList.toggle("show");
}