import React from "react";

function TeethAnalysis() {
  return (
    <div
      style={{
        backgroundColor: "#fff",
        borderRadius: "8px",
        padding: "1rem",
        textAlign: "center",
      }}
    >
      <h4>우악상단을 더 꼼꼼히!</h4>
      <img
        src="https://cdn-icons-png.flaticon.com/512/616/616408.png"
        alt="teeth"
        style={{ width: "100px", margin: "1rem auto" }}
      />
      <p>
        홍길동님은 현재 전체적으로 <span style={{ color: "blue" }}>건강</span>
        하지만, <strong>우악상단</strong>이 상대적으로 취약해요.
      </p>
    </div>
  );
}

export default TeethAnalysis;
